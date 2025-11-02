import 'package:alarm/model/voice_tag_settings.dart';
import 'package:alarm/model/volume_settings.dart';
import 'package:alarm/src/generated/platform_bindings.g.dart';
import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'edit_ringing_alarm_settings.g.dart';

/// 修改响铃中闹铃状态
@JsonSerializable(constructor: '_')
class EditRingingAlarmSettings extends Equatable {
  /// Internal constructor for [EditRingingAlarmSettings].
  const EditRingingAlarmSettings({
    required this.id,
    this.volumeSettings,
    this.loopAudio,
    this.assetAudioPath,
    this.vibrate,
    this.flashlight,
    this.voiceTagSettings,
  });

  const EditRingingAlarmSettings._({
    required this.id,
    this.volumeSettings,
    this.loopAudio,
    this.assetAudioPath,
    this.vibrate,
    this.flashlight,
    this.voiceTagSettings,
  });

  /// Converts the JSON object to a `EditRingingAlarmSettings` instance.
  factory EditRingingAlarmSettings.fromJson(Map<String, dynamic> json) =>
      _$EditRingingAlarmSettingsFromJson(json);

  /// id
  final int id;

  /// 音量控制
  final VolumeSettings? volumeSettings;

  /// 音频路径
  final String? assetAudioPath;

  /// 循环播放
  final bool? loopAudio;

  /// 震动
  final bool? vibrate;

  /// 是否开启手电筒
  final bool? flashlight;

  /// 语音标签设置
  final VoiceTagSettings? voiceTagSettings;

  /// Converts the [EditRingingAlarmSettings] instance to a JSON object.
  Map<String, dynamic> toJson() => _$EditRingingAlarmSettingsToJson(this);

  @override
  List<Object?> get props => [
        id,
        volumeSettings,
        assetAudioPath,
        loopAudio,
        vibrate,
        flashlight,
        voiceTagSettings
      ];

  /// Converts to wire datatype which is used for host platform communication.
  EditRingingAlarmSettingsWire toWire() => EditRingingAlarmSettingsWire(
        id: id,
        volumeSettings: volumeSettings?.toWire(),
        assetAudioPath: assetAudioPath,
        loopAudio: loopAudio,
        vibrate: vibrate,
        flashlight: flashlight,
        voiceTagSettings: voiceTagSettings?.toWire(),
      );
}
