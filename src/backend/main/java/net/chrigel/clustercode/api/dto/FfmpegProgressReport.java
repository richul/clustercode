package net.chrigel.clustercode.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.chrigel.clustercode.api.ProgressReport;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(
        description = "A progress report specifically for ffmpeg based cluster members. All properties return -1 if" +
        " no conversion is currently active.")
public class FfmpegProgressReport implements ProgressReport {

    @ApiModelProperty(
            value = "The current bitrate of the conversion in kbit/s.",
            example = "2543")
    private Double bitrate;

    @ApiModelProperty(
            value = "The amount of processed frames so far in the conversion process.",
            example = "2851")
    private Long frame;

    @ApiModelProperty(
            value = "The duration of the media in milliseconds.",
            example = "1337545")
    private Long duration;

    @ApiModelProperty(
            value = "The processed time of the media in milliseconds (cursor). Between 0 and $duration.",
            example = "161960"
    )
    private Long time;

    @ApiModelProperty(
            value = "The file size of the currently created output file in MB.",
            example = "20.34")
    private Double size;

    @ApiModelProperty(
            value = "The current amount of frames per second encoding.",
            example = "33")
    private Double fps;

    @ApiModelProperty(value = "The current percentage of the conversion progress.",
            example = "12.1087")
    private Double percentage;
}
