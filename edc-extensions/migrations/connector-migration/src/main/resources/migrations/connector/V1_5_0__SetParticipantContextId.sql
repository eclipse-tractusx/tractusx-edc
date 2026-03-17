UPDATE edc_asset SET participant_context_id = '${ParticipantContextId}';
UPDATE edc_contract_agreement SET agr_participant_context_id = '${ParticipantContextId}';
UPDATE edc_contract_definitions SET participant_context_id = '${ParticipantContextId}';
UPDATE edc_contract_negotiation SET participant_context_id = '${ParticipantContextId}';
UPDATE edc_edr_entry SET participant_context_id = '${ParticipantContextId}';
UPDATE edc_policy_monitor SET participant_context_id = '${ParticipantContextId}';
UPDATE edc_policydefinitions SET participant_context_id = '${ParticipantContextId}';
UPDATE edc_transfer_process SET participant_context_id = '${ParticipantContextId}';
UPDATE edc_data_plane SET participant_context_id = '${ParticipantContextId}';

-- UPDATE edc_data_plane_instance SET data = data || '{"participantContextId": "${ParticipantContextId}"}'::json;
UPDATE edc_data_plane_instance SET data = (data::jsonb || '{"participantContextId": "${ParticipantContextId}"}'::jsonb)::json;