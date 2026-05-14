import java.util.List;

/**
 * Ett litet, körbart exempel som modellerar en Java 25-beslutsmatris.
 *
 * Exemplet använder inte preview-API:er, JNI eller Unsafe. Det är avsiktligt:
 * syftet är att visa hur ett team kan göra riskbeslut explicita innan
 * produktionssättning.
 *
 * Kör:
 *   javac --release 25 SecurityAdoptionMatrix.java
 *   java SecurityAdoptionMatrix
 */
public class SecurityAdoptionMatrix {

    enum Status {
        FINAL,
        PREVIEW,
        EXPERIMENTAL,
        UNSUPPORTED,
        STANDARD_BUT_RISKY
    }

    enum Recommendation {
        ADOPT,
        LAB_ONLY,
        INVESTIGATE,
        REPLACE,
        EXPLICIT_APPROVAL_REQUIRED
    }

    record Decision(
            String area,
            Status status,
            String risk,
            String owner,
            Recommendation recommendation,
            boolean blocksProduction) {
    }

    public static void main(String[] args) {
        var decisions = List.of(
                new Decision(
                        "KDF API",
                        Status.FINAL,
                        "Cryptographic behavior must be verified with test vectors",
                        "security-team",
                        Recommendation.INVESTIGATE,
                        false),
                new Decision(
                        "PEM API",
                        Status.PREVIEW,
                        "Preview API can change and requires preview flags",
                        "platform-team",
                        Recommendation.LAB_ONLY,
                        false),
                new Decision(
                        "sun.misc.Unsafe in dependencies",
                        Status.UNSUPPORTED,
                        "Future compatibility and integrity risk",
                        "dependency-owner",
                        Recommendation.REPLACE,
                        true),
                new Decision(
                        "JNI/native access for observability agent",
                        Status.STANDARD_BUT_RISKY,
                        "Native library loading must be explicit and documented",
                        "platform-team",
                        Recommendation.EXPLICIT_APPROVAL_REQUIRED,
                        false)
        );

        printReport(decisions);
    }

    private static void printReport(List<Decision> decisions) {
        System.out.println("Java 25 security and production decision matrix");
        System.out.println("================================================");

        for (Decision decision : decisions) {
            System.out.printf("%nArea: %s%n", decision.area());
            System.out.printf("  Status: %s%n", decision.status());
            System.out.printf("  Risk: %s%n", decision.risk());
            System.out.printf("  Owner: %s%n", decision.owner());
            System.out.printf("  Recommendation: %s%n", decision.recommendation());
            System.out.printf("  Blocks production: %s%n",
                    decision.blocksProduction() ? "YES" : "no");
        }

        long blockers = decisions.stream()
                .filter(Decision::blocksProduction)
                .count();

        System.out.printf("%nProduction blockers: %d%n", blockers);
    }
}
