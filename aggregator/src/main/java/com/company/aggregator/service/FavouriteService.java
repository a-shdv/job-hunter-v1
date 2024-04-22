package com.company.aggregator.service;

import com.company.aggregator.exception.FavouriteAlreadyExistsException;
import com.company.aggregator.exception.FavouriteNotFoundException;
import com.company.aggregator.exception.FavouritesIsEmptyException;
import com.company.aggregator.entity.Favourite;
import com.company.aggregator.entity.User;
import com.company.aggregator.repository.FavouriteRepository;
import com.company.aggregator.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class  FavouriteService {
    private final FavouriteRepository favouriteRepository;
    private final UserRepository userRepository;

    @Transactional
    public Page<Favourite> findFavourites(User user, PageRequest pageRequest) {
        return favouriteRepository.findByUser(user, pageRequest).get();
    }

    @Transactional
    public void addToFavourites(User user, Favourite favourite) throws FavouriteAlreadyExistsException {
        if (favouriteRepository.findByUserAndSource(user, favourite.getSource()).isPresent()) {
            throw new FavouriteAlreadyExistsException("Вакансия уже существует в избранном " + favourite.getSource());
        }
        favourite.setUser(user);
        favouriteRepository.save(favourite);
    }

    @Transactional
    public void deleteFromFavourites(User user, Long id) throws FavouriteNotFoundException {
        Optional<Favourite> favourite = favouriteRepository.findById(id);
        if (favourite.isEmpty()) {
            throw new FavouriteNotFoundException("Вакансия не найдена!");
        }
        List<Favourite> favourites = favouriteRepository.findByUser(user).get();
        favourites.remove(favourite.get());
        user.setFavourites(favourites);
        userRepository.save(user);
        favouriteRepository.deleteById(id);
    }

    @Transactional
    public void deleteFavourites(User user) {
        List<Favourite> favourites = favouriteRepository.findByUser(user).get();
        favourites.clear();
        user.setFavourites(favourites);
        userRepository.save(user);
    }

    @Transactional
    public List<Favourite> findByUser(User user) throws FavouritesIsEmptyException {
        return favouriteRepository.findByUser(user).orElseThrow( () -> new FavouritesIsEmptyException("Список избранных вакансий пуст!"));
    }
}
