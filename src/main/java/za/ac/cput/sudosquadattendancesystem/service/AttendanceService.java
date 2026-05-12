package za.ac.cput.sudosquadattendancesystem.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.ac.cput.sudosquadattendancesystem.domain.AttendanceLog;
import za.ac.cput.sudosquadattendancesystem.domain.HardwarePayload;
import za.ac.cput.sudosquadattendancesystem.repository.AttendanceLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final Set<String> VALID_RFID_TAGS = Set.of(
            "A1B2C3D4",  // Student Card 1
            "E5F6A7B8",  // Student Card 2
    );

    //Fingerprint template slot IDs enrolled on the AS608 sensor module.
    private static final Set<Integer> VALID_FINGERPRINT_IDS = Set.of(
            1,  // Enrolled finger: slot 1 on the AS608
            2   // Enrolled finger: slot 2 on the AS608
    );


    private final AttendanceLogRepository attendanceLogRepository;

    public AttendanceLog processScan(HardwarePayload payload) {

        log.info("Scan received — node: '{}', RFID: '{}', Fingerprint: {}",
                payload.getNodeId(), payload.getRfidTag(), payload.getFingerprintId());

        boolean isAuthorised = validateCredentials(payload.getRfidTag(), payload.getFingerprintId());

        AttendanceLog logEntry = AttendanceLog.builder()
                .hardwareNodeId(payload.getNodeId())
                .rfidTagId(payload.getRfidTag())
                .fingerprintId(payload.getFingerprintId())
                .scanTimestamp(LocalDateTime.now())
                .accessGranted(isAuthorised)
                .build();

        AttendanceLog saved = attendanceLogRepository.save(logEntry);

        log.info("Scan logged (id={}) — outcome: {}",
                saved.getId(), isAuthorised ? "ACCESS_GRANTED" : "ACCESS_DENIED");

        return saved;
    }

    public List<AttendanceLog> getAllLogs() {
        return attendanceLogRepository.findAllByOrderByScanTimestampDesc();
    }

    private boolean validateCredentials(String rfidTag, Integer fingerprintId) {
        boolean rfidValid        = rfidTag != null       && VALID_RFID_TAGS.contains(rfidTag);
        boolean fingerprintValid = fingerprintId != null && VALID_FINGERPRINT_IDS.contains(fingerprintId);

        return rfidValid || fingerprintValid;
    }
}
