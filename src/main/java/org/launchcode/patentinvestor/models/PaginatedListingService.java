package org.launchcode.patentinvestor.models;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kamdem
 * Generic version of the PaginatedListingService class.
 * Service to generate the paginated "item" list
 * for the requested page using
 * the Spring Data Commons library
 *
 * @param <T> the type of the object being listed with pagination
 */
@Service
public class PaginatedListingService<T> {
    private List<T> listOfItems = new ArrayList<T>();

    public Page<T> findPaginated(Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<T> list;

        if (listOfItems.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, listOfItems.size());
            list = listOfItems.subList(startItem, toIndex);
        }

        Page<T> itemPage
                = new PageImpl<T>(list, PageRequest.of(currentPage, pageSize), listOfItems.size());
        return itemPage;
    }

    /*public List<T> getListOfItems() {
        return listOfItems;
    }*/

    public void setListOfItems(List<T> listOfItems) {
        this.listOfItems = listOfItems;
    }

    public static List<String> paginating(int currentPage, int numberOfPage) {
        int current = currentPage,
                lastPage = numberOfPage,
                delta = 4,
                left = current - delta,
                right = current + delta + 1;
        List<String> range = new ArrayList<>();
        List<String> rangeWithDots = new ArrayList<>();
        int l = 0;

        for (int i = 1; i <= lastPage; i++) {
            if (i == 1 || i == lastPage || i >= left && i < right) {
                range.add("" + i);
            }
        }

        for (String i : range) {
            if (l > 0) {
                if (Integer.parseInt(i) - l == 2) {
                    rangeWithDots.add("" + (l + 1));
                } else if (Integer.parseInt(i) - l != 1) {
                    rangeWithDots.add("...");
                }
            }
            rangeWithDots.add(i);
            l = Integer.parseInt(i);
        }

        return rangeWithDots;
    }
}
