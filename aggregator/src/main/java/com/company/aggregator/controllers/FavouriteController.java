package com.company.aggregator.controllers;

import com.company.aggregator.dtos.FavouriteDto;
import com.company.aggregator.models.Favourite;
import com.company.aggregator.models.User;
import com.company.aggregator.services.EmailSenderService;
import com.company.aggregator.services.FavouriteService;
import com.company.aggregator.services.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
@RequestMapping("/favourites")
@Slf4j
public class FavouriteController {
    private final FavouriteService favouriteService;
    private final EmailSenderService emailSenderService;
    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping
    public String findFavourites(@AuthenticationPrincipal User user,
                                 @RequestParam(required = false, defaultValue = "0") int page,
                                 @RequestParam(required = false, defaultValue = "5") int size,
                                 Model model) {
        String success = (String) model.getAttribute("success");
        String error = (String) model.getAttribute("error");
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        Page<Favourite> favourites = favouriteService.findFavourites(user, PageRequest.of(page, size));
        model.addAttribute("favourites", favourites);
        return "favourites/favourites";
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<String>> addToFavourites(@AuthenticationPrincipal User user,
                                                                     @ModelAttribute("favouriteDto") FavouriteDto favouriteDto) {
        CompletableFuture<Void> future = favouriteService.addToFavouritesAsync(user, FavouriteDto.toFavourite(favouriteDto));

        return future.thenApply(result -> ResponseEntity.ok("Success!"))
                .exceptionally(ex -> {
                    log.error(ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ex.getMessage());
                });
    }


    @PostMapping("/{id}")
    public ResponseEntity<String> deleteFromFavourites(@AuthenticationPrincipal User user, @PathVariable Long id) {
        CompletableFuture<Void> future = favouriteService.deleteFromFavouritesAsync(user, id);
        return future.thenApply((res) -> ResponseEntity.ok("Success!"))
                .exceptionally(ex ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ex.getMessage())).join();
    }

    @PostMapping("/clear")
    public ResponseEntity<String> deleteVacancies(@AuthenticationPrincipal User user) {
        favouriteService.deleteFavouritesAsync(user).join();
        return ResponseEntity.ok().body("Vacancies cleared successfully");
    }
}
