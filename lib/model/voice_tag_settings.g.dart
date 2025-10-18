// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'voice_tag_settings.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

VoiceTagSettings _$VoiceTagSettingsFromJson(Map<String, dynamic> json) =>
    $checkedCreate(
      'VoiceTagSettings',
      json,
      ($checkedConvert) {
        final val = VoiceTagSettings._(
          enable: $checkedConvert('enable', (v) => v as bool),
          text: $checkedConvert('text', (v) => v as String? ?? ''),
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

Map<String, dynamic> _$VoiceTagSettingsToJson(VoiceTagSettings instance) =>
    <String, dynamic>{
      'enable': instance.enable,
      'text': instance.text,
      'volume': instance.volume,
      'speechRate': instance.speechRate,
      'pitch': instance.pitch,
      'loop': instance.loop,
      'loopInterval': instance.loopInterval,
    };
