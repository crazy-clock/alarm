// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'edit_ringing_alarm_settings.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

EditRingingAlarmSettings _$EditRingingAlarmSettingsFromJson(
        Map<String, dynamic> json) =>
    $checkedCreate(
      'EditRingingAlarmSettings',
      json,
      ($checkedConvert) {
        final val = EditRingingAlarmSettings._(
          id: $checkedConvert('id', (v) => (v as num).toInt()),
          volumeSettings: $checkedConvert(
              'volumeSettings',
              (v) => v == null
                  ? null
                  : VolumeSettings.fromJson(v as Map<String, dynamic>)),
          loopAudio: $checkedConvert('loopAudio', (v) => v as bool?),
          assetAudioPath:
              $checkedConvert('assetAudioPath', (v) => v as String?),
          vibrate: $checkedConvert('vibrate', (v) => v as bool?),
          flashlight: $checkedConvert('flashlight', (v) => v as bool?),
          voiceTagSettings: $checkedConvert(
              'voiceTagSettings',
              (v) => v == null
                  ? null
                  : VoiceTagSettings.fromJson(v as Map<String, dynamic>)),
        );
        return val;
      },
    );

Map<String, dynamic> _$EditRingingAlarmSettingsToJson(
        EditRingingAlarmSettings instance) =>
    <String, dynamic>{
      'id': instance.id,
      if (instance.volumeSettings?.toJson() case final value?)
        'volumeSettings': value,
      if (instance.assetAudioPath case final value?) 'assetAudioPath': value,
      if (instance.loopAudio case final value?) 'loopAudio': value,
      if (instance.vibrate case final value?) 'vibrate': value,
      if (instance.flashlight case final value?) 'flashlight': value,
      if (instance.voiceTagSettings?.toJson() case final value?)
        'voiceTagSettings': value,
    };
