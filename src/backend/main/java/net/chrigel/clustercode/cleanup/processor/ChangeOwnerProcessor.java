package net.chrigel.clustercode.cleanup.processor;

import lombok.extern.slf4j.XSlf4j;
import net.chrigel.clustercode.cleanup.CleanupContext;
import net.chrigel.clustercode.cleanup.CleanupProcessor;
import net.chrigel.clustercode.cleanup.CleanupSettings;
import net.chrigel.clustercode.cleanup.impl.CleanupModule;
import net.chrigel.clustercode.process.ExternalProcessService;
import net.chrigel.clustercode.process.ProcessConfiguration;
import net.chrigel.clustercode.util.InvalidConfigurationException;
import net.chrigel.clustercode.util.Platform;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@XSlf4j
public class ChangeOwnerProcessor implements CleanupProcessor {

    private final CleanupSettings cleanupSettings;
    private final ExternalProcessService externalProcessService;
    private boolean enabled;

    @Inject
    ChangeOwnerProcessor(
        CleanupSettings cleanupSettings,
        ExternalProcessService externalProcessService) {
        this.cleanupSettings = cleanupSettings;
        this.externalProcessService = externalProcessService;
        cleanupSettings.getUserId().ifPresent(this::checkId);
        cleanupSettings.getGroupId().ifPresent(this::checkId);
        checkSettings();
    }

    private void checkSettings() {
        enabled = true;
        if (!cleanupSettings.getGroupId().isPresent() || !cleanupSettings.getUserId().isPresent()) {
            log.warn("User and/or Group ID not set. Please set {} and {} accordingly. This strategy will be ignored.",
                CleanupModule.CLEANUP_OWNER_GROUP_KEY, CleanupModule.CLEANUP_OWNER_USER_KEY);
            enabled = false;
        }
        if (Platform.currentPlatform() == Platform.WINDOWS) {
            log.warn("Changing owner on Windows is not supported. This strategy will be ignored.");
            enabled = false;
        }
    }

    @Override
    public CleanupContext processStep(CleanupContext context) {
        log.entry(context);

        if (!isStepValid(context)) return log.exit(context);

        Path outputFile = context.getOutputPath();
        List<String> args = buildArguments(outputFile);

        log.info("Changing owner of {} to {}.", outputFile, args.get(0));

        ProcessConfiguration config = ProcessConfiguration
            .builder()
            .executable(Paths.get("/bin", "chown"))
            .arguments(args)
            .stdoutObserver(observable -> observable.subscribe(System.out::println))
            .build();

        externalProcessService
            .start(config)
            .subscribe(exitCode -> {
                if (exitCode > 0) log.warn(
                    "Could not change owner of {}. Exit code of 'chown' with arguments {} was {}.",
                    outputFile, args, exitCode);
            }, log::catching);

        return log.exit(context);
    }

    private boolean isStepValid(CleanupContext context) {
        if (!enabled) {
            log.debug("This processor is disabled.");
            return false;
        }

        if (context.getOutputPath() == null) {
            log.warn("Output file has not been created yet. Are you sure you have " +
                "configured the cleanup strategies correctly?");
            return false;
        }
        return true;
    }

    private List<String> buildArguments(Path outputFile) {
        return Arrays.asList(
            cleanupSettings.getUserId().orElse(65534) + ":" + cleanupSettings.getGroupId().orElse(65534),
            outputFile.toString());
    }

    private void checkId(int id) {
        if (0 > id) {
            throw new InvalidConfigurationException("Cannot use a negative owner id");
        } else if (id > 65534) {
            throw new InvalidConfigurationException("Cannot use an owner id higher than 65534");
        }
    }
}
