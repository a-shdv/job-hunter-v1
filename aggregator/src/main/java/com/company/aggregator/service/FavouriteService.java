package com.company.aggregator.service;

import com.company.aggregator.model.Favourite;
import com.company.aggregator.model.User;
import com.company.aggregator.repository.FavouriteRepository;
import com.company.aggregator.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class FavouriteService {
    private final FavouriteRepository favouriteRepository;
    private final UserRepository userRepository;

    @Transactional
    public CompletableFuture<Page<Favourite>> findFavouritesAsync(User user, PageRequest pageRequest) {
        return CompletableFuture.supplyAsync(() -> favouriteRepository.findByUser(user, pageRequest));
    }

    @Transactional
    public CompletableFuture<Void> addToFavouritesAsync(User user, Favourite favourite) {
        return CompletableFuture.runAsync(() -> {
            favourite.setUser(user);
            favouriteRepository.save(favourite);
        });
    }

    @Transactional
    public CompletableFuture<Void> deleteFavourites(User user) {
        return CompletableFuture.runAsync(() -> {
            List<Favourite> favourites = favouriteRepository.findByUser(user);
            favourites.clear();
            user.setFavourites(favourites);
            userRepository.save(user);
        });
    }

}
