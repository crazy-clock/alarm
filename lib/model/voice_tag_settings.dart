import 'package:alarm/src/generated/platform_bindings.g.dart';
import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'voice_tag_settings.g.dart';

/// 语音标签设置
@JsonSerializable(constructor: '_')
class VoiceTagSettings extends Equatable {
  /// Internal constructor for [VoiceTagSettings].
  const VoiceTagSettings({
    required this.enable,
    this.text = '',
    this.volume = 1.0,
    this.speechRate = 0.1,
    this.pitch = 1.0,
  });

  const VoiceTagSettings._({
    required this.enable,
    this.text = '',
    this.volume = 1.0,
    this.speechRate = 0.1,
    this.pitch = 1.0,
  });

  /// Converts the JSON object to a `VolumeSettings` instance.
  factory VoiceTagSettings.fromJson(Map<String, dynamic> json) => _$VoiceTagSettingsFromJson(json);

  /// 是否启用
  final bool enable;

  /// 内容
  final String text;

  /// 音量
  final double volume;

  /// 语速
  final double speechRate;

  /// 音调
  final double pitch;

  /// Converts the [VoiceTagSettings] instance to a JSON object.
  Map<String, dynamic> toJson() => _$VoiceTagSettingsToJson(this);

  @override
  List<Object?> get props => [enable, volume, speechRate, pitch, text];

  /// Converts to wire datatype which is used for host platform communication.
  VoiceTagSettingsWire toWire() => VoiceTagSettingsWire(
        enable: enable,
        text: text,
        volume: volume,
        speechRate: speechRate,
        pitch: pitch,
      );
}
