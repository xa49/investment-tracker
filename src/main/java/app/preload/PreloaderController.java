package app.preload;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-api")
@AllArgsConstructor
public class PreloaderController {

    private final Preloader preloader;

    @GetMapping("/preload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void preload() {
        preloader.preload();
    }

    @GetMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        preloader.reset();
    }
}
