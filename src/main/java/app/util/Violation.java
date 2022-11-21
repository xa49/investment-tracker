package app.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Violation {
    private String field;
    private String message;
}
