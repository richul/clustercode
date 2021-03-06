package net.chrigel.clustercode.scan.impl;

import net.chrigel.clustercode.cleanup.CleanupSettings;
import net.chrigel.clustercode.scan.Media;
import net.chrigel.clustercode.scan.MediaScanSettings;
import net.chrigel.clustercode.test.FileBasedUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

public class MediaScanServiceImplTest implements FileBasedUnitTest {

    private MediaScanServiceImpl subject;
    private Path inputDir;

    @Mock
    private MediaScanSettings scanSettings;
    @Mock
    private CleanupSettings cleanupSettings;

    private Map<Path, List<Media>> candidates;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        setupFileSystem();
        when(scanSettings.getAllowedExtensions()).thenReturn(Arrays.asList(".mp4"));
        when(scanSettings.getBaseInputDir()).thenReturn(getPath("input"));
        when(scanSettings.getSkipExtension()).thenReturn(".done");
        when(cleanupSettings.getMarkSourceDirectory()).thenReturn(Optional.of(getPath("mark")));

        inputDir = scanSettings.getBaseInputDir();
        subject = new MediaScanServiceImpl(scanSettings, FileScannerImpl::new, cleanupSettings);
    }

    @Test
    public void getListOfMediaFiles_ShouldReturnListWithTwoEntries() throws Exception {
        Path dir1 = inputDir.resolve("1");

        Path file11 = createFile(dir1.resolve("file11.mp4"));
        Path file12 = createFile(dir1.resolve("file12.mp4"));

        List<Media> result = subject.getListOfMediaFiles(dir1);

        assertThat(result).extracting(Media::getSourcePath)
                .containsExactly(inputDir.relativize(file11), inputDir.relativize(file12));
    }

    @Test
    public void getListOfMediaFiles_ShouldReturnListWithOneEntry_AndIgnoreCompanionFile() throws Exception {
        Path dir1 = inputDir.resolve("1");

        Path file11 = createFile(dir1.resolve("file11.mp4"));
        createFile(dir1.resolve("file12.mp4"));
        createFile(dir1.resolve("file12.mp4.done"));
        createFile(dir1.resolve("file13.txt"));

        List<Media> result = subject.getListOfMediaFiles(dir1);

        assertThat(result).extracting(Media::getSourcePath).containsExactly(inputDir.relativize(file11));
    }

    @Test
    public void getListOfMediaFiles_ShouldReturnListWithOneEntry_AndIgnoreMarkedFileInMarkSourceDir() throws Exception {
        Path dir1 = getPath("input","1");

        Path file11 = createFile(dir1.resolve("file11.mp4"));
        createFile(dir1.resolve("file12.mp4"));
        createFile(cleanupSettings.getMarkSourceDirectory().get().resolve("1").resolve("file12.mp4.done"));

        List<Media> result = subject.getListOfMediaFiles(dir1);

        assertThat(result).extracting(Media::getSourcePath).containsExactly(inputDir.relativize(file11));
    }

    @Test
    public void getListOfMediaFiles_ShouldReturnEmptyList_IfNoFilesFound() throws Exception {
        Path dir1 = createDirectory(inputDir.resolve("1"));

        List<Media> result = subject.getListOfMediaFiles(dir1);

        assertThat(result).isEmpty();
    }

    @Test
    public void retrieveFiles_ShouldReturnOneEntry_AndIgnoreInvalidDirectories() throws Exception {
        Path dir1 = createDirectory(inputDir.resolve("1"));
        createDirectory(inputDir.resolve("inexistent"));

        candidates = subject.retrieveFiles();

        assertThat(candidates).containsKey(dir1);
        assertThat(candidates.get(dir1)).isEmpty();
        assertThat(candidates).hasSize(1);
    }

    @Test
    public void doExecute_ShouldThrowException_IfInputDirIsInexistent() throws Exception {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> subject.retrieveFiles());
    }

    @Test
    public void isPriorityDirectory_ShouldReturnTrue_IfDirectoryIsPositive() throws Exception {
        Path dir = inputDir.resolve("2");
        assertThat(subject.isPriorityDirectory(dir)).isTrue();
    }

    @Test
    public void isPriorityDirectory_ShouldReturnTrue_IfDirectoryIsZero() throws Exception {
        Path dir = inputDir.resolve("0");
        assertThat(subject.isPriorityDirectory(dir)).isTrue();
    }

    @Test
    public void isPriorityDirectory_ShouldReturnFalse_IfDirectoryIsInvalid() throws Exception {
        Path dir = inputDir.resolve("-1");
        assertThat(subject.isPriorityDirectory(dir)).isFalse();
    }

    @Test
    public void getNumberFromDir_ShouldReturn2_IfPathBeginsWith2() throws Exception {
        Path dir = inputDir.resolve("2");
        assertThat(subject.getNumberFromDir(dir)).isEqualTo(2);
    }

    @Test
    public void getNumberFromDir_ShouldThrowException_IfPathDoesNotContainNumber() throws Exception {
        Path dir = inputDir.resolve("error");
        assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> subject.getNumberFromDir(dir));
    }
}
