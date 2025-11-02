import 'package:alarm/src/generated/platform_bindings.g.dart';
import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'time_pressure_settings.g.dart';

/// 时间压力设置
@JsonSerializable(constructor: '_')
class TimePressureSettings extends Equatable {
  /// Internal constructor for [TimePressureSettings].
  const TimePressureSettings({
    required this.enable,
    this.volume = 1.0,
    this.speechRate = 0.1,
    this.pitch = 1.0,
    this.loop = false,
    this.loopInterval = 1000,
  });

  const TimePressureSettings._({
    required this.enable,
    this.volume = 1.0,
    this.speechRate = 0.1,
    this.pitch = 1.0,
    this.loop = false,
    this.loopInterval = 1000,
  });

  /// Converts the JSON object to a `VolumeSettings` instance.
  factory TimePressureSettings.fromJson(Map<String, dynamic> json) =>
      _$TimePressureSettingsFromJson(json);

  /// 是否启用
  final bool enable;

  /// 音量
  final double volume;

  /// 语速
  final double speechRate;

  /// 音调
  final double pitch;

  /// 是否循环播放
  final bool loop;

  /// 循环播放间隔
  final int loopInterval;

  /// Converts the [TimePressureSettings] instance to a JSON object.
  Map<String, dynamic> toJson() => _$TimePressureSettingsToJson(this);

  @override
  List<Object?> get props =>
      [enable, volume, speechRate, pitch, loop, loopInterval];

  /// Converts to wire datatype which is used for host platform communication.
  TimePressureSettingsWire toWire() => TimePressureSettingsWire(
        enable: enable,
        volume: volume,
        speechRate: speechRate,
        pitch: pitch,
        loop: loop,
        loopInterval: loopInterval,
      );
}
