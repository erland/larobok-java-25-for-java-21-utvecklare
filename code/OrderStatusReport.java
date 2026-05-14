import module java.base;

class OrderStatusReport {
    void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java OrderStatusReport.java <status-file>");
            return;
        }

        var statusFile = Path.of(args[0]);

        try (var lines = Files.lines(statusFile)) {
            var countByStatus = lines
                    .map(String::strip)
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            TreeMap::new,
                            Collectors.counting()));

            countByStatus.forEach((status, count) ->
                    System.out.println(status + ": " + count));
        }
    }
}
