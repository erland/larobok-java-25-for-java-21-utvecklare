// Syfte: Exempelverktyg för kapitel 3.
// Status: Labbkod för Java 25 compact source files.

void main() {
    var orderIds = List.of("A-10042", "A-10043", "B-20001", "");

    IO.println("OrderFlow order check");
    IO.println("---------------------");

    for (var id : orderIds) {
        IO.println(display(id) + " -> " + statusFor(id));
    }
}

String statusFor(String orderId) {
    if (orderId == null || orderId.isBlank()) {
        return "invalid";
    }

    if (!orderId.matches("[A-Z]-\\d+")) {
        return "invalid";
    }

    return orderId.startsWith("A-") ? "standard" : "manual-review";
}

String display(String orderId) {
    return orderId == null || orderId.isBlank() ? "<blank>" : orderId;
}
