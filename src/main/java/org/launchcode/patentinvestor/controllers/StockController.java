package org.launchcode.patentinvestor.controllers;

import org.launchcode.patentinvestor.data.StockRepository;
import org.launchcode.patentinvestor.data.TagRepository;
import org.launchcode.patentinvestor.models.*;
import org.launchcode.patentinvestor.models.dto.StockTagDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by kamdem
 */
@Controller
@RequestMapping("stocks")
public class StockController {

    List<Stock> listOfStocksFound;
    static final int numberOfItemsPerPage = 10;
    static final String baseColor = "white";
    static final String selectedColor = "green";
    static String sortCriteria = "";
    static String searchDestinationUrl = "/stocks/searchResults";

    static HashMap<String, String> columnChoices = new HashMap<>();
    static HashMap<String, String> stockExchanges = new HashMap<>();

    public StockController() {
        columnChoices.put("all", "All");
        columnChoices.put("ticker", "Ticker");
        columnChoices.put("companyName", "Company Name");

        stockExchanges.put("NAS", "Nasdaq Stock Market");
        stockExchanges.put("NYS", "New York Stock Exchange");
        stockExchanges.put("PSE", "Pacific Stock Exchange");
        stockExchanges.put("ASE", "American Stock Exchange");
        stockExchanges.put("BATS", "Bats Global Markets");
        stockExchanges.put("OTC", "Over-the-counter");
    }

    @ModelAttribute(name = "columns")
    public static HashMap<String, String> getColumnChoices() {
        return columnChoices;
    }

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PaginatedListingService<Stock> paginatedListingService;

    /**
     * Display initial empty "Stock search form"
     *
     * This method returns at URL /stocks/search
     */
    @RequestMapping(value = "search", method = RequestMethod.GET)
    public String displaySearchForm(Model model) {
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("columns", columnChoices);
        model.addAttribute("title", "Search stock");
        return "stocks/search";
    }

    /**
     * Display ALL stocks SEARCH results (with pagination)
     *
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     *
     * "listingResults" works with: stockPage
     *
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     *
     * This method returns at URL /stocks/search
     */
    //@PostMapping(value = "searchResults")
    @RequestMapping(value = "searchResults", method = {RequestMethod.GET, RequestMethod.POST})
    public String displaySearchResults(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String searchType,
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        String iconsDestinationUrl = "/stocks/reorderedResults/?sortIcon=";
        String paginationDestinationUrl = "/stocks/searchResults/?size=";

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        model.addAttribute("currentPage", currentPage);

        if (searchTerm != null) {
            if (searchTerm.toLowerCase().equals("all") || searchTerm == "") {
                //Release ALL stocks
                model.addAttribute("title", "All Stocks");
                listOfStocksFound = new ArrayList<Stock>((List<Stock>) stockRepository.findAll());
            } else {
                listOfStocksFound = StockData.findByColumnAndValue(searchType, searchTerm, (List<Stock>) stockRepository.findAll());
                //used to display the form with previous radio button in search.html
                model.addAttribute("searchType", searchType);
            }
        }

        model.addAttribute("listOfStocksFound", listOfStocksFound);
        paginatedListingService.setListOfItems(listOfStocksFound);

        //used to display the form in search.html
        model.addAttribute("columns", columnChoices);

        model.addAttribute("title", "Found stock(s)");

        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            //model.addAttribute("pageNumbers", pageNumbers);
            List<String> reducedPagination = Comparators.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/search";
        //return "redirect:/search/results";
    }

    /**
     * Display ALL stocks search results (with pagination)
     * /stocks/results
     * Precondition : listOfStocksFound contains values
     *
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     *
     * "listingResults" works with: stockPage
     *
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     *
     * This method returns at URL /stocks/search
     */
    @GetMapping(value = "reorderedResults")
    public String displayReorderedSearchResults(
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        sortCriteria = sortIcon.orElse(sortCriteria);
        String iconsDestinationUrl = "/stocks/reorderedResults/?sortIcon=";
        String paginationDestinationUrl = "/stocks/reorderedResults/?size=";

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("sortIcon", sortCriteria);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        model.addAttribute("currentPage", currentPage);

        switch (sortCriteria) {
            case "tickerUp":
                listOfStocksFound.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                listOfStocksFound.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                listOfStocksFound.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                listOfStocksFound.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                listOfStocksFound.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                listOfStocksFound.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
        }

        model.addAttribute("listOfStocksFound", listOfStocksFound);
        paginatedListingService.setListOfItems(listOfStocksFound);

        //used in (fragments :: listingResults) in search.html
        //model.addAttribute("stocks", stocks);

        //used to display the form in search.html
        model.addAttribute("columns", columnChoices);
        model.addAttribute("title", "Found stock(s)");

        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            //model.addAttribute("pageNumbers", pageNumbers);
            List<String> reducedPagination = Comparators.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/search";
        //return "redirect:/search/results";
    }

    /**
     * Display ALL stocks OR stocks relevant
     * to a specific tagId (with pagination)
     *
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     *
     * "listingResults" works with: stockPage
     *
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     *
     * This method returns at URL /stocks/index
     * */
    @RequestMapping(method = RequestMethod.GET)
    public String displayStocks(
            @RequestParam(required = false) Integer tagId,
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        sortCriteria = sortIcon.orElse(sortCriteria);
        String iconsDestinationUrl = "/stocks/?sortIcon=";
        String paginationDestinationUrl = "/stocks/?size=";

        model.addAttribute("sortIcon", sortCriteria);
        model.addAttribute("baseColor", baseColor);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        model.addAttribute("currentPage", currentPage);

        if (tagId == null) { // display ALL stocks
            model.addAttribute("title", "All Stocks");
            listOfStocksFound = (List<Stock>) stockRepository.findAll();
            //model.addAttribute("stocks", stockRepository.findAll());
        } else { // display stocks for tagId
            Optional<Tag> result = tagRepository.findById(tagId);
            if (result.isEmpty()) {
                listOfStocksFound = new ArrayList<Stock>();//null;
                model.addAttribute("title", "Invalid Tag ID: " + tagId);

                return "stocks/index";
            } else { // there are stocks for that tag!
                Tag tag = result.get();
                model.addAttribute("title", "All Stocks for: " + tag.getName());
                listOfStocksFound = tag.getStocks();
                //model.addAttribute("stocks", tag.getStocks());
            }
        }

        switch (sortCriteria) {
            case "tickerUp":
                listOfStocksFound.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                listOfStocksFound.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                listOfStocksFound.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                listOfStocksFound.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                listOfStocksFound.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                listOfStocksFound.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
        }

        paginatedListingService.setListOfItems(listOfStocksFound);
        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            List<String> reducedPagination = Comparators.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }

        return "stocks/index";
    }

    /*
    @GetMapping("create")
    public String displayCreateStockForm(Model model) {
        model.addAttribute("title", "Create Stock");
        model.addAttribute(new Stock());
        model.addAttribute("categories", stockCategoryRepository.findAll());
        return "stocks/create";
    }

    @PostMapping("create")
    public String processCreateStockForm(@ModelAttribute @Valid Stock newStock,
                                         Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("title", "Create Stock");
            return "stocks/create";
        }

        stockRepository.save(newStock);
        return "redirect:";
    }*/

    /*
    @GetMapping("delete")
    public String displayDeleteStockForm(Model model) {
        model.addAttribute("title", "Delete Stocks");
        model.addAttribute("stocks", stockRepository.findAll());
        return "stocks/delete";
    }

    @PostMapping("delete")
    public String processDeleteStocksForm(@RequestParam(required = false) int[] stockIds) {

        if (stockIds != null) {
            for (int id : stockIds) {
                stockRepository.deleteById(id);
            }
        }

        return "redirect:";
    }*/

    /**
     * Display details of a stock
     * This method returns at URL /stocks/detail
     * */
    @GetMapping("detail/{stockId}")
    public String displayStockDetails(@PathVariable Integer stockId, Model model) {
        Optional<Stock> result = stockRepository.findById(stockId);
        if (result.isEmpty()) {
            model.addAttribute("title", "Invalid Stock ID: " + stockId);
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            model.addAttribute("title",  "Summary of '" + stock.getTicker() + "'");
            model.addAttribute("stock", stock);
            model.addAttribute("exchangePlatforms", stockExchanges);
        }
        return "stocks/detail";
    }

    // add tag to a specific stock : responds to /stocks/add-tag?stockId=13
    @GetMapping("add-tag")
    public String displayAddTagForm(@RequestParam Integer stockId,
                                    Model model) {
        Optional<Stock> result = stockRepository.findById(stockId);
        Stock stock = result.get();
        model.addAttribute("title", "Add Tag to: " + stock.getStockDetails().getCompanyName());
        model.addAttribute("tags", tagRepository.findAll());
        model.addAttribute("stock", stock);
        StockTagDTO stockTagDTO = new StockTagDTO();
        stockTagDTO.setStock(stock);
        model.addAttribute("stockTag", stockTagDTO);
        return "stocks/add-tag";
    }

    // responds to /stocks/add-tag?stockId=13
    @PostMapping("add-tag")
    public String processAddTagForm(@ModelAttribute @Valid StockTagDTO stockTag,
                                    Errors errors,
                                    Model model) {
        if (!errors.hasErrors()) {
            Stock stock = stockTag.getStock();
            Tag tag = stockTag.getTag();
            if (!stock.getTags().contains(tag)) {
                stock.addTag(tag);
                stockRepository.save(stock);
            }
            return "redirect:detail?stockId=" + stock.getId();
        }
        return "redirect:add-tag?stockId=" + stockTag.getStock().getId();
    }

}
