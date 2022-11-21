package app.broker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBrokerCommand {

    @Pattern(regexp = "^[a-zA-Z0-9+$€!.@\\- ]+$")
    private String name;
}
