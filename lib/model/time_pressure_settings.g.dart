// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'time_pressure_settings.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TimePressureSettings _$TimePressureSettingsFromJson(
        Map<String, dynamic> json) =>
    $checkedCreate(
      'TimePressureSettings',
      json,
      ($checkedConvert) {
        final val = TimePressureSettings._(
          enable: $checkedConvert('enable', (v) => v as bool),
          volume:
              $checkedConvert('volume', (v) => (v as num?)?.toDouble() ?? 1.0),
          speechRate: $checkedConvert(
              'speechRate', (v) => (v as num?)?.toDouble() ?? 0.1),
          pitch:
              $checkedConvert('pitch', (v) => (v as num?)?.toDouble() ?? 1.0),
          loop: $checkedConvert('loop', (v) => v as bool? ?? false),
          loopInterval: $checkedConvert(
              'loopInterval', (v) => (v as num?)?.toInt() ?? 1000),
        );
        return val;
      },
    );

Map<String, dynamic> _$TimePressureSettingsToJson(
        TimePressureSettings instance) =>
    <String, dynamic>{
      'enable': instance.enable,
      'volume': instance.volume,
      'speechRate': instance.speechRate,
      'pitch': instance.pitch,
      'loop': instance.loop,
      'loopInterval': instance.loopInterval,
    };
