package com.pravin.appeal.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * SYNTHETIC demo data - invented patients, fictional insurers, made-up chart notes.
 * In a real product this would be the existing claims / chart / authorization services.
 *
 * The four claims are chosen so each one takes a different path through the workflow:
 *  C-1001 MRI, "not medically necessary", strong evidence      -> full appeal
 *  C-1002 Physical therapy visit 13, no authorization          -> AI advises NOT to appeal
 *  C-1003 Office visit + injection, missing modifier           -> routed to "corrected claim"
 *  C-1004 Echocardiogram, "not medically necessary", strong    -> full appeal
 */
@Component
public class DemoData {

    public record Patient(String id, String name, LocalDate dateOfBirth) { }

    public record DeniedClaim(String id, String patientId, String insurer, String service, String serviceCodes,
                              LocalDate serviceDate, BigDecimal amount, String denialCode, String denialRemark,
                              LocalDate deniedOn, int appealWindowDays) {

        public LocalDate appealDeadline() {
            return deniedOn.plusDays(appealWindowDays);
        }

        public long daysLeft() {
            return ChronoUnit.DAYS.between(LocalDate.now(), appealDeadline());
        }
    }

    public record ChartEntry(String patientId, LocalDate date, String type, String text, String source) { }

    public record Authorization(String patientId, String service, String status, String details) { }

    private static final List<Patient> PATIENTS = List.of(
            new Patient("P-01", "Maria Lopez", LocalDate.of(1974, 3, 12)),
            new Patient("P-02", "James Carter", LocalDate.of(1966, 11, 2)),
            new Patient("P-03", "Aisha Khan", LocalDate.of(1988, 6, 25)),
            new Patient("P-04", "Robert Chen", LocalDate.of(1959, 1, 30)));

    private static final List<DeniedClaim> CLAIMS = List.of(
            new DeniedClaim("C-1001", "P-01", "Sunrise Health Plan", "MRI lumbar spine without contrast", "72148",
                    LocalDate.of(2026, 9, 2), new BigDecimal("1240.00"), "CO-50",
                    "These are non-covered services because this is not deemed a medical necessity by the payer.",
                    LocalDate.of(2026, 10, 6), 60),
            new DeniedClaim("C-1002", "P-02", "Lakeside Mutual", "Physical therapy session (visit 13)", "97110",
                    LocalDate.of(2026, 10, 3), new BigDecimal("145.00"), "CO-197",
                    "Precertification/authorization/notification absent.",
                    LocalDate.of(2026, 10, 15), 60),
            new DeniedClaim("C-1003", "P-03", "Sunrise Health Plan", "Office visit + knee joint injection (same day)", "99213, 20610",
                    LocalDate.of(2026, 9, 18), new BigDecimal("310.00"), "CO-4",
                    "The procedure code is inconsistent with the modifier used or a required modifier is missing.",
                    LocalDate.of(2026, 10, 9), 90),
            new DeniedClaim("C-1004", "P-04", "Lakeside Mutual", "Echocardiogram (heart ultrasound)", "93306",
                    LocalDate.of(2026, 9, 22), new BigDecimal("890.00"), "CO-50",
                    "These are non-covered services because this is not deemed a medical necessity by the payer.",
                    LocalDate.of(2026, 10, 12), 60));

    private static final List<ChartEntry> CHART = List.of(
            // P-01 Maria Lopez - back pain, MRI
            new ChartEntry("P-01", LocalDate.of(2026, 7, 1), "Office visit", "Low back pain radiating to the left leg, started 1 week ago. Started NSAIDs, referred to physical therapy.", "Visit note, Dr. Patel"),
            new ChartEntry("P-01", LocalDate.of(2026, 7, 8), "Imaging", "X-ray lumbar spine: mild degenerative changes, no fracture.", "Radiology report"),
            new ChartEntry("P-01", LocalDate.of(2026, 7, 10), "Physical therapy", "PT session 1 of 6. Pain 7/10.", "PT note"),
            new ChartEntry("P-01", LocalDate.of(2026, 8, 14), "Physical therapy", "PT session 6 of 6. No improvement, pain 7/10. Therapist recommends physician review.", "PT note"),
            new ChartEntry("P-01", LocalDate.of(2026, 8, 26), "Office visit", "8 weeks of symptoms despite 6 PT sessions and NSAIDs. Positive straight-leg raise on the left, reduced left ankle reflex, numbness in L5 area. MRI ordered.", "Visit note, Dr. Patel"),
            // P-02 James Carter - physical therapy beyond authorized visits
            new ChartEntry("P-02", LocalDate.of(2026, 8, 1), "Office visit", "Right shoulder rotator cuff strain. Physical therapy ordered.", "Visit note, Dr. Moore"),
            new ChartEntry("P-02", LocalDate.of(2026, 9, 30), "Physical therapy", "PT session 12. Good progress, range of motion 80% of normal.", "PT note"),
            new ChartEntry("P-02", LocalDate.of(2026, 10, 3), "Physical therapy", "PT session 13. Continued strengthening exercises.", "PT note"),
            // P-03 Aisha Khan - separate problem on the same day as a procedure
            new ChartEntry("P-03", LocalDate.of(2026, 9, 18), "Office visit", "Planned knee injection for known osteoarthritis. Patient also reported NEW frequent headaches for 3 weeks - separately evaluated: history, neurological exam, blood pressure check, plan for headache diary and follow-up.", "Visit note, Dr. Singh"),
            new ChartEntry("P-03", LocalDate.of(2026, 9, 18), "Procedure", "Right knee corticosteroid injection performed without complications.", "Procedure note, Dr. Singh"),
            // P-04 Robert Chen - echocardiogram
            new ChartEntry("P-04", LocalDate.of(2026, 9, 1), "Office visit", "Shortness of breath on exertion for 3 weeks. New systolic heart murmur on exam. Ankle swelling.", "Visit note, Dr. Garcia"),
            new ChartEntry("P-04", LocalDate.of(2026, 9, 1), "Test result", "ECG: left ventricular hypertrophy pattern, abnormal.", "ECG report"),
            new ChartEntry("P-04", LocalDate.of(2026, 9, 15), "Office visit", "Symptoms persist. Echocardiogram ordered to evaluate murmur and possible heart failure.", "Visit note, Dr. Garcia"));

    private static final List<Authorization> AUTHORIZATIONS = List.of(
            new Authorization("P-01", "MRI lumbar spine", "Not required", "Sunrise Health Plan does not require prior authorization for this MRI; it reviews medical necessity after the service."),
            new Authorization("P-02", "Physical therapy", "Approved - 12 visits", "Approved 12 visits from 2026-08-01 to 2026-10-31. Visits 1-12 used by 2026-09-30. No authorization requested for visits beyond 12."),
            new Authorization("P-04", "Echocardiogram", "Not required", "Lakeside Mutual does not require prior authorization for echocardiograms."));

    // Insurer coverage policies, keyed by "insurer|first service code"
    private static final Map<String, String> POLICIES = Map.of(
            "Sunrise Health Plan|72148", "Lumbar MRI is covered when: (1) low back pain with leg (radicular) symptoms for at least 6 weeks AND failure of at least 6 weeks of conservative therapy (physical therapy and/or medication); OR (2) neurological deficits found on examination. A plain X-ray is recommended before MRI.",
            "Lakeside Mutual|97110", "Physical therapy beyond 12 visits requires a new prior authorization BEFORE the visit. Retroactive authorization is granted only for emergencies.",
            "Sunrise Health Plan|99213", "An office visit on the same day as a minor procedure is payable only with modifier 25, and only when the note documents a significant, separately identifiable problem.",
            "Lakeside Mutual|93306", "Transthoracic echocardiogram is covered for: a new heart murmur, symptoms suggesting heart failure (e.g. shortness of breath, swelling), or an abnormal ECG.");

    public List<DeniedClaim> claims() {
        return CLAIMS;
    }

    public Optional<DeniedClaim> claim(String claimId) {
        return CLAIMS.stream().filter(c -> c.id().equals(claimId)).findFirst();
    }

    public Patient patient(String patientId) {
        return PATIENTS.stream().filter(p -> p.id().equals(patientId)).findFirst().orElseThrow();
    }

    public List<ChartEntry> chart(String patientId) {
        return CHART.stream().filter(e -> e.patientId().equals(patientId)).toList();
    }

    public List<Authorization> authorizations(String patientId) {
        return AUTHORIZATIONS.stream().filter(a -> a.patientId().equals(patientId)).toList();
    }

    public String policy(DeniedClaim claim) {
        String firstCode = claim.serviceCodes().split(",")[0].trim();
        return POLICIES.getOrDefault(claim.insurer() + "|" + firstCode, "No published policy found for this service.");
    }
}
