# Payload Android Library

An Android library for integrating [Payload](https://payload.co).

## Installation

Download the [latest](https://github.com/payload-code/payload-android/archive/master.zip)
version from GitHub.

### 2) Include in Project

Include the folder in your Android Studio project as a module.

## Get Started

Once you've included the Payload Java library in your project,
include the `com.payload.pl` and `com.payload.android.Payload` namespace to get started.

All Payload objects and methods are accessible using the `pl` static class.

For more information, please refer to the [Payload Java Libaray](https://github.com/payload-code/payload-java).

```java
import com.payload.pl;
import com.payload.android.Payload;
```

### API Authentication

To authenticate with the Payload API, you'll need a live or test API key. API
keys are accessible from within the Payload dashboard.

```java
import com.payload.pl;

pl.api_key = "client_key_3bW9JMZtPVDOfFNzwRdfE";
```

### Creating an Object


Interfacing with the Payload API is done primarily through Payload Objects.
The Payload Android library extends helper functions to simplify Android's
restrictions on HTTP requests originating from the main thread.
Below is an example of
creating a payment using the `pl.Payment` object with the Payload Android helper functions.


```java
// Create a Payment
Payload.create(new pl.Payment(){{
    set("amount", 100.0);
    set("payment_method", new pl.Card(){{
        set("card_number, "4242 4242 4242 4242");
    }});
}}).then((pl.Payment payment) -> {
    if (payment.getStr("status") == "processed")
        handleSuccess(payment);
});
```

### Void a Payment

```java
// Create a Payment
Payload.update(payment, pl.attr("status", "voided") )
    .then(((pl.Payment voided) -> {
        handleVoid(payment);
    });
```

### Select Customers

```java
Payload.all(pl.Customer.filter_by(
    pl.attr("email").eq("matt.perez@example.com")
)).then((List<pl.Customer> custs) -> {
    handleCustomers(customers);
});
```

### Get a Specific Customer

```java
Payload.get(pl.Customer.class, cust_id).then((pl.Customer cust) -> {
    handleCustomer(cust);
});
```

### Handle Request Errors

```java
Payload.create(new pl.Customer(){{
    set("email", "matt perez@example.com");
    set("name", "Matt Perez");
}}).then((pl.Customer cust) -> {
    handleNewCust(cust);
}).error((Exceptions.PayloadError error) -> {
    handleError(error);
});
```

## Documentation

To get further information on Payload's Android library and API capabilities,
visit the unabridged [Payload Documentation](https://docs.payload.co/).
