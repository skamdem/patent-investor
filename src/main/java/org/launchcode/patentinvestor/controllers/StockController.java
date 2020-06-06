package org.launchcode.patentinvestor.controllers;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.launchcode.patentinvestor.data.StockRepository;
import org.launchcode.patentinvestor.data.StockShareRepository;
import org.launchcode.patentinvestor.data.TagRepository;
import org.launchcode.patentinvestor.models.Comparators;
import org.launchcode.patentinvestor.models.*;
import org.launchcode.patentinvestor.models.dto.StockTagDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.launchcode.patentinvestor.controllers.HomeController.*;
import static org.launchcode.patentinvestor.models.ApiData.*;

/**
 * Created by kamdem
 */
@Controller
@RequestMapping("stocks")
@SessionAttributes("portfolio")
public class StockController {

    //General messages
    private final String INFO_MESSAGE_KEY = "message";

    //Number of tags per stock
    private final int MAX_NUMBER_OF_TAGS = 3;

    //About deleted tags from stocks
    private final String ACTION_MESSAGE_KEY = "secondMessage";

    List<Stock> listOfStocksFoundInSearch;
    List<Stock> listOfStocksFoundInStocksListing;

    //used for pagination
    static final int numberOfItemsPerPage = 6;//10;

    static final String baseColor = "white";
    static final String selectedColor = "green";
    static String searchDestinationUrl = "/stocks/searchResults";

    String sortCriteriaInSearch = "";
    String sortCriteriaInStocksListings = "";
    String sortCriteriaInPortfolio = "";

    //Helps view download progress
    int numberOfUsptoItemsDownloaded = 0;
    int numberOfIexItemsDownloaded = 0;

    /**
     * initialize to 1 in order to prevent division by zero
     * divisor to compute percentage
     */
    int globalSize = 1;

    //In page to search a stock
    static HashMap<String, String> columnChoices = new HashMap<>();

    //Customize private listing exchange display in stock details page
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
    private StockShareRepository stockShareRepository;

    @Autowired
    private PaginatedListingService<Stock> paginatedListingService;

    @Autowired
    private PaginatedListingService<Stock> subTypePaginatedListingService;

    @Autowired
    AuthenticationController authenticationController;

    @ModelAttribute("portfolio")
    public Portfolio portfolio(HttpServletRequest request) {
        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is logged in
            return loggedInUser.getPortfolio();
        }
        return null;
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(HttpServletRequest request) {
        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is logged in
            return true;
        }
        return false;
    }

    /**
     * Display initial empty "Stock search form"
     * <p>
     * This method returns at URL /stocks/search
     */
    @RequestMapping(value = "search", method = RequestMethod.GET)
    public String displaySearchForm(
            Model model,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is logged in
            model.addAttribute("isLoggedIn", true);
        }

        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("columns", columnChoices);
        model.addAttribute("title", "Search stock");
        return "stocks/search";
    }

    /**
     * Display ALL stocks SEARCH results (with pagination)
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/search
     *
     * @param searchTerm
     * @param searchType
     * @param model
     * @param page
     * @param size
     * @return
     */
    //@PostMapping(value = "searchResults")
    @RequestMapping(value = "searchResults", method = {RequestMethod.GET, RequestMethod.POST})
    public String displaySearchResults(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String searchType,
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is logged in
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("portfolio", loggedInUser.getPortfolio());
        }

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);

        String iconsDestinationUrl = "/stocks/reorderedResults/?sortIcon=";
        String paginationDestinationUrl = "/stocks/searchResults/?size=";
        if (searchTerm != null) {
            iconsDestinationUrl = "/stocks/reorderedResults/?searchType=" + searchType + "&searchTerm=" + searchTerm + "&sortIcon=";
            paginationDestinationUrl = "/stocks/searchResults/?searchType=" + searchType + "&searchTerm=" + searchTerm + "&size=";
        }

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("sortIcon", sortCriteriaInSearch);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        //model.addAttribute("currentPage", currentPage);

        if (searchTerm != null) {
            if (searchTerm.toLowerCase().equals("all") || searchTerm == "") {
                //Release ALL stocks
                model.addAttribute("title", "All Stocks");
                listOfStocksFoundInSearch = new ArrayList<Stock>((List<Stock>) stockRepository.findAll());
            } else {
                listOfStocksFoundInSearch = StockData.findByColumnAndValue(searchType, searchTerm, (List<Stock>) stockRepository.findAll());
            }
        }

//        System.out.println(searchTerm);
        //used to display the form with previous radio button in search.html
        model.addAttribute("searchType", searchType);
        model.addAttribute("searchTerm", searchTerm);

        model.addAttribute("listOfStocksFound", listOfStocksFoundInSearch);
        paginatedListingService.setListOfItems(listOfStocksFoundInSearch);

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
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/search";
        //return "redirect:/search/results";
    }

    /**
     * Display ALL stocks search results (with pagination)
     * Precondition : listOfStocksFound contains values
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/search
     *
     * @param model
     * @param page
     * @param size
     * @param sortIcon
     * @param searchTerm
     * @param searchType
     * @return
     */
    @GetMapping(value = "reorderedResults")
    public String displayReorderedSearchResults(
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String searchType,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is logged in
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("portfolio", loggedInUser.getPortfolio());
        }

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        sortCriteriaInSearch = sortIcon.orElse(sortCriteriaInSearch);
        //System.out.println(sortCriteriaInSearch);
        String iconsDestinationUrl = "/stocks/reorderedResults/?sortIcon=";
        String paginationDestinationUrl = "/stocks/reorderedResults/?size=";
        if (searchTerm != null) {
            iconsDestinationUrl = "/stocks/reorderedResults/?searchType=" + searchType + "&searchTerm=" + searchTerm + "&sortIcon=";
            paginationDestinationUrl = "/stocks/reorderedResults/?searchType=" + searchType + "&searchTerm=" + searchTerm + "&size=";
        }

        //used to display the form with previous radio button in search.html
        model.addAttribute("searchType", searchType);
        model.addAttribute("searchTerm", searchTerm);

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("sortIcon", sortCriteriaInSearch);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        //model.addAttribute("currentPage", currentPage);

        switch (sortCriteriaInSearch) {
            case "tickerUp":
                listOfStocksFoundInSearch.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                listOfStocksFoundInSearch.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                listOfStocksFoundInSearch.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                listOfStocksFoundInSearch.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                listOfStocksFoundInSearch.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                listOfStocksFoundInSearch.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                listOfStocksFoundInSearch.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                listOfStocksFoundInSearch.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
        }

        model.addAttribute("listOfStocksFound", listOfStocksFoundInSearch);
        paginatedListingService.setListOfItems(listOfStocksFoundInSearch);

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
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/search";
        //return "redirect:/search/results";
    }

    /**
     * Display ALL stocks OR stocks relevant
     * to a specific tagId (with pagination)
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/index
     *
     * @param tagId
     * @param model
     * @param page
     * @param size
     * @param sortIcon
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public String displayStocks(
            @RequestParam(required = false) Integer tagId,
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is properly logged in
            //model.addAttribute("isLoggedIn", true);
            //System.out.println(model.containsAttribute("isLoggedIn"));
            System.out.println("BEFORE: " + model.containsAttribute("portfolio"));
            model.addAttribute("portfolio", loggedInUser.getPortfolio());
            System.out.println("AFTER: " + model.containsAttribute("portfolio"));
        }

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);

        sortCriteriaInStocksListings = sortIcon.orElse(sortCriteriaInStocksListings);
        String iconsDestinationUrl = "/stocks/?sortIcon=";
        String paginationDestinationUrl = "/stocks/?size=";

        //Add IP30's number of patents and weighted price to model
        if (((List<Stock>) stockRepository.findAll()).size() < 30) {
            //model.addAttribute(ACTION_MESSAGE_KEY, "info|Not enough stocks in the database to make up IP30");
            model.addAttribute("noIP30", "noIP30");
        } else {
            loadIP30PriceAndPatentsFootprint(model);
        }

        if (tagId == null) { // display ALL stocks
            model.addAttribute("title", "All Stocks");
            listOfStocksFoundInStocksListing = (List<Stock>) stockRepository.findAll();
        } else { // display stocks for tagId
            Optional<Tag> result = tagRepository.findById(tagId);
            if (result.isEmpty()) {
                listOfStocksFoundInStocksListing = new ArrayList<Stock>();//null;
                model.addAttribute("title", "Invalid Tag ID: " + tagId);
                //return "stocks/index";
            } else { // there are stocks for that tag!
                Tag tag = result.get();
                model.addAttribute("title", "Investment field: " + tag.getDisplayName());
                listOfStocksFoundInStocksListing = tag.getStocks();
                model.addAttribute("tag", tag);
                model.addAttribute(INFO_MESSAGE_KEY, "info|" +
                        tag.getDisplayName() +
                        " is currently set to " +
                        tag.getStocks().size() + " stock(s).");

                //adjust page destination to stay on this tag page
                iconsDestinationUrl = "/stocks/?tagId=" + tagId + "&sortIcon=";
                paginationDestinationUrl = "/stocks/?tagId=" + tagId + "&size=";
            }
        }

        if (listOfStocksFoundInStocksListing.size() == 0) {
            model.addAttribute(INFO_MESSAGE_KEY, "info|No stocks were found!");
        }
        switch (sortCriteriaInStocksListings) {
            case "tickerUp":
                listOfStocksFoundInStocksListing.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                listOfStocksFoundInStocksListing.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                listOfStocksFoundInStocksListing.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                listOfStocksFoundInStocksListing.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                listOfStocksFoundInStocksListing.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                listOfStocksFoundInStocksListing.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                listOfStocksFoundInStocksListing.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                listOfStocksFoundInStocksListing.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
        }
//System.out.println("number of stocks = "+listOfStocksFoundInStocksListing.size());
        paginatedListingService.setListOfItems(listOfStocksFoundInStocksListing);
        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));

        model.addAttribute("stockPage", stockPage);
        model.addAttribute("sortIcon", sortCriteriaInStocksListings);
        model.addAttribute("baseColor", baseColor);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        //model.addAttribute("currentPage", currentPage);

        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
//System.out.println("totalPages = "+totalPages);
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/index";
    }

    /**
     * BOTH LOGGED IN AND NOT LOGGED IN
     * Display details of a stock
     * This method returns at URL /stocks/detail
     */
    @GetMapping("detail/{stockId}")
    public String displayStockDetails(
            @PathVariable Integer stockId,
            Model model,
            HttpServletRequest request) {

        Optional<Stock> result = stockRepository.findById(stockId);
        if (result.isEmpty()) {
            model.addAttribute("title", "Invalid Stock ID: " + stockId);
            model.addAttribute(INFO_MESSAGE_KEY, "danger|No matching stock found!");
            return "redirect:/";
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            model.addAttribute("title", "Summary of '" + stock.getTicker() + "'");
            model.addAttribute("stock", stock);
            model.addAttribute("exchangePlatforms", stockExchanges);

            User loggedInUser = authenticationController.getUserFromSession(request.getSession());
            if (loggedInUser != null) {//user is properly logged in
                model.addAttribute("isLoggedIn", true);

                Portfolio portfolio = loggedInUser.getPortfolio();
//                Set<Tag> relevantTags = portfolio.getTags().stream()
//                        .distinct()
//                        .filter(stock.getTags()::contains)
//                        .collect(Collectors.toSet());

                //intersection of the lists of portfolio tags and all stock tags
                List<Tag> relevantTags = stock.getInPortfolioTags(portfolio.getTags());
                model.addAttribute("relevantTags", relevantTags);
                if (portfolio.contains(stock)) {
                    model.addAttribute(INFO_MESSAGE_KEY, "info|The stock '" + stock.getTicker() + "' is currently in your " +
                            PORTFOLIO_LINK_IN_MSG);
                    model.addAttribute("inPortfolio", true);
                    model.addAttribute("numberOfShares", portfolio.getRelevantNumberOfShares(stock));
                }
                model.addAttribute("MAX_NUMBER_OF_TAGS", MAX_NUMBER_OF_TAGS);
            }

            loadAddtionalStockDataFromIexApi(stock.getTicker(), model);
        }
        return "stocks/detail";
    }

    /**
     * ONLY LOGGED IN USER
     * Display details of my portfolio
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/portfolio
     *
     * @param model
     * @param page
     * @param size
     * @param sortIcon
     * @param request
     * @return
     */
    @GetMapping("portfolio")
    public String displayPortfolioOfStocks(
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        }

        //user is logged in at this point
        Portfolio portfolio = loggedInUser.getPortfolio();
        model.addAttribute("isLoggedIn", true);
        //To collect numberOfShares, totalNumberOfPatents, netWorth, adjustedPatents
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("portfolioPage", true);
        //model.addAttribute("adjustedPatents", portfolio.getAdjustedPatents());

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        sortCriteriaInPortfolio = sortIcon.orElse(sortCriteriaInPortfolio);
        String iconsDestinationUrl = "/stocks/portfolio/?sortIcon=";
        String paginationDestinationUrl = "/stocks/portfolio/?size=";

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("sortIcon", sortCriteriaInPortfolio);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);

        model.addAttribute("title", "Stocks portfolio of " +
                loggedInUser.getUsername());

        List<StockShare> portfolioStockShareList = portfolio.getStockShares();
        List<Stock> portfolioList = portfolio.getRelevantStocks();

        switch (sortCriteriaInPortfolio) {
            case "tickerUp":
                portfolioList.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                portfolioList.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                portfolioList.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                portfolioList.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                portfolioList.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                portfolioList.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                portfolioList.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                portfolioList.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
            case "sharesUp":
                portfolioStockShareList.sort(Comparators.numberOfSharesComparator);
                List<String> tickerListUp = portfolioStockShareList.stream().map(s -> s.getStock().getTicker()).collect(Collectors.toList());
                Collections.sort(portfolioList, (s1, s2) ->
                        Integer.compare(tickerListUp.indexOf(s1.getTicker())
                                - tickerListUp.indexOf(s2.getTicker()), 0));
                break;
            case "sharesDown":
                portfolioStockShareList.sort(Comparators.numberOfSharesComparator.reversed());
                List<String> tickerListDown = portfolioStockShareList.stream().map(s -> s.getStock().getTicker()).collect(Collectors.toList());
                Collections.sort(portfolioList, (s1, s2) ->
                        Integer.compare(tickerListDown.indexOf(s1.getTicker())
                                - tickerListDown.indexOf(s2.getTicker()), 0));
                break;
            case "percentInPortfolioUp":
                portfolioStockShareList.sort(Comparators.percentInPortfolioComparator);
                List<String> tickerListPercentUp = portfolioStockShareList.stream().map(s -> s.getStock().getTicker()).collect(Collectors.toList());
                Collections.sort(portfolioList, (s1, s2) ->
                        Integer.compare(tickerListPercentUp.indexOf(s1.getTicker())
                                - tickerListPercentUp.indexOf(s2.getTicker()), 0));
                break;
            case "percentInPortfolioDown":
                portfolioStockShareList.sort(Comparators.percentInPortfolioComparator.reversed());
                List<String> tickerListPercentDown = portfolioStockShareList.stream().map(s -> s.getStock().getTicker()).collect(Collectors.toList());
                Collections.sort(portfolioList, (s1, s2) ->
                        Integer.compare(tickerListPercentDown.indexOf(s1.getTicker())
                                - tickerListPercentDown.indexOf(s2.getTicker()), 0));
                break;
        }

        model.addAttribute(INFO_MESSAGE_KEY, "info|Click on the number of shares to adjust it for each stock");
        subTypePaginatedListingService.setListOfItems(portfolioList);
        Page<Stock> stockPage = subTypePaginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            List<String> reducedPagination = subTypePaginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }

        return "stocks/portfolio";
    }

    /**
     * ONLY LOGGED IN
     *
     * @param model
     * @param stockId
     * @return
     */
    @GetMapping("adjust-shares-portfolio/{stockId}")
    public String displayAdjustSharesInPortfolio(
            Model model,
            @PathVariable int stockId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        } else {//user is logged in
            model.addAttribute("isLoggedIn", true);
        }

        Stock stock = stockRepository.findById(stockId).get();
        String ticker = stock.getTicker();
        model.addAttribute("title",
                "Edit number of shares for " + ticker);
        model.addAttribute("stock", stock);
        model.addAttribute(INFO_MESSAGE_KEY, "info|The" +
                " number of shares must be a number between 0 and 1000");
        model.addAttribute("stockId", stockId);

        List<StockShare> portfolioStockShareList = loggedInUser.getPortfolio().getStockShares();
        for (StockShare stockShare : portfolioStockShareList) {
            if (stockShare.getStock() == stock) {
                model.addAttribute("stockShare", stockShare);
                break;
            }
        }

        return "stocks/adjust-shares";
    }

    /**
     * ONLY LOGGED IN
     *
     * @param stockShareId
     * @param numberOfShares
     * @param redirectAttributes
     * @return
     */
    @PostMapping("adjust-shares-portfolio")
    public String processAdjustSharesInPortfolio(
            int stockShareId,
            int numberOfShares,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        }

        StockShare stockShare = stockShareRepository.findById(stockShareId).get();
        stockShare.setNumberOfShares(numberOfShares);
        stockShareRepository.save(stockShare);
        redirectAttributes.addFlashAttribute(ACTION_MESSAGE_KEY, "success|The" +
                " number of shares has been updated successfully for " + stockShare.getStock().getTicker());
        return "redirect:/stocks/portfolio";
    }

    /**
     * ONLY LOGGED IN
     * Display details of a stock AFTER adding
     * it to the portfolio
     * This method returns at URL /stocks/detail
     *
     * Precondition: User is logged in!
     */
    @GetMapping("add-to-portfolio/{stockId}")
    public String displayStockDetailsInPortfolio(
            @PathVariable Integer stockId,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        }
        //user is logged in at this point
        model.addAttribute("isLoggedIn", true);

        Optional<Stock> result = stockRepository.findById(stockId);
        if (result.isEmpty()) {
            model.addAttribute("title", "Invalid Stock ID: " + stockId);
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            Portfolio portfolio = loggedInUser.getPortfolio();

//            Set<Tag> relevantTags = portfolio.getTags().stream()
//                    .distinct()
//                    .filter(stock.getTags()::contains)
//                    .collect(Collectors.toSet());
            List<Tag> relevantTags = stock.getInPortfolioTags(portfolio.getTags());
            model.addAttribute("relevantTags", relevantTags);

            if (!portfolio.contains(stock)) {//stock in NOT in portfolio
                StockShare stockShare = new StockShare(stock, portfolio);
                stockShareRepository.save(stockShare);

                model.addAttribute("inPortfolio", true);
                model.addAttribute("numberOfShares", portfolio.getRelevantNumberOfShares(stock));

                model.addAttribute("title", "Summary of '" + stock.getTicker() + "'");
                model.addAttribute("stock", stock);
                model.addAttribute("MAX_NUMBER_OF_TAGS", MAX_NUMBER_OF_TAGS);
                model.addAttribute("exchangePlatforms", stockExchanges);
                loadAddtionalStockDataFromIexApi(stock.getTicker(), model);
                model.addAttribute(ACTION_MESSAGE_KEY, "success|The stock '" + stock.getTicker() + "' has been added to your " +
                        PORTFOLIO_LINK_IN_MSG);
            } else {
                redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "warning|The stock '" + stock.getTicker() + "' is already in your " +
                        PORTFOLIO_LINK_IN_MSG);
                return "redirect:";
            }
        }
        return "stocks/detail";
    }

    /**
     * This method retrieves current data from remote IEX API
     *
     * @param ticker
     * @param model
     */
    void loadAddtionalStockDataFromIexApi(
            String ticker,
            Model model) {
        // /stock/{symbol}/company
        String iexStockUrlString = BASE_URL_LIVE_IEX //BASE_URL_SANDBOX_IEX
                + "/stable/stock/" + ticker + "/company?token=" + IEX_PUBLIC_TOKEN;//IEX_TEST_TOKEN_TPK;

        // /stock/{symbol}/logo
        String iexStockLogoUrlString = BASE_URL_LIVE_IEX //BASE_URL_SANDBOX_IEX
                + "/stable/stock/" + ticker + "/logo?token=" + IEX_PUBLIC_TOKEN;//IEX_TEST_TOKEN_TPK;

        RestTemplate iexRestTemplate = new RestTemplate();
        String iexApiQueryResult = iexRestTemplate.getForObject(iexStockUrlString, String.class);
        String iexApiLogoQueryResult = iexRestTemplate.getForObject(iexStockLogoUrlString, String.class);

        try {
            Object iexObj = new JSONParser().parse(iexApiQueryResult);
            JSONObject iexJo = (JSONObject) iexObj; //typecasting iexObj to JSONObject
            model.addAttribute("website", iexJo.get("website"));
            model.addAttribute("description", iexJo.get("description"));
            model.addAttribute("industry", iexJo.get("industry"));
            model.addAttribute("CEO", iexJo.get("CEO"));
            model.addAttribute("sector", iexJo.get("sector"));
            model.addAttribute("employees", iexJo.get("employees"));
            model.addAttribute("address", iexJo.get("address"));
            model.addAttribute("state", iexJo.get("state"));
            model.addAttribute("city", iexJo.get("city"));
            model.addAttribute("zip", iexJo.get("zip"));
            model.addAttribute("country", iexJo.get("country"));
            model.addAttribute("phone", iexJo.get("phone"));

            iexObj = new JSONParser().parse(iexApiLogoQueryResult);
            iexJo = (JSONObject) iexObj; //typecasting iexObj to JSONObject
            model.addAttribute("logoUrl", iexJo.get("url"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * ONLY LOGGED IN
     * Display details of a stock AFTER
     * removing a stock from portfolio
     * This method returns at URL /stocks/detail
     *
     * @param stockId
     * @param redirectAttributes
     * @return
     */
    @GetMapping("remove-from-portfolio/{stockId}")
    public String displayStockDetailsRemovedFromPortfolio(
            @PathVariable Integer stockId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        }

        Optional<Stock> result = stockRepository.findById(stockId);
        if (result.isEmpty()) {
            //model.addAttribute("title", "Invalid Stock ID: " + stockId);
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|Invalid Stock ID: " + stockId);
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            Portfolio portfolio = loggedInUser.getPortfolio();
            if (!portfolio.contains(stock)) {//the stock is not in the portfolio
                redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|The stock '" + stock.getTicker() + "' is not currently in your " +
                        PORTFOLIO_LINK_IN_MSG);
            } else {//The stock to be removed is in the portfolio
                //i) identify corresponding stockShare
                //ii) delete corresponding stockShare
                List<StockShare> listOfStockShare = portfolio.getStockShares();
                for (StockShare stockShare : listOfStockShare) {
                    if (stockShare.getStock().getTicker().equals(stock.getTicker())) {
                        stockShareRepository.deleteById(stockShare.getId());
                        redirectAttributes.addFlashAttribute(ACTION_MESSAGE_KEY, "success|The stock '" + stock.getTicker() + "' has been removed from your " +
                                PORTFOLIO_LINK_IN_MSG +
                                ". All shares previously set to this stock have been wiped out.");
                        break;
                    }
                }
            }
//            model.addAttribute("title", "Summary of '" + stock.getTicker() + "'");
//            model.addAttribute("stock", stock);
//            model.addAttribute("exchangePlatforms", stockExchanges);
//            loadAddtionalStockDataFromIexApi(stock.getTicker(), model);
        }
        return "redirect:/stocks/portfolio";//"stocks/detail";
    }

    /**
     * ONLY LOGGED IN
     * add tag to a stock
     * responds to /stocks/add-tag?stockId=13
     *
     * @param stockId
     * @param model
     * @return
     */
    @GetMapping("add-tag/{stockId}")
    public String displayAddTagForm(
            @PathVariable Integer stockId,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        } else {//user is logged in
            model.addAttribute("isLoggedIn", true);
        }

        Optional<Stock> result = stockRepository.findById(stockId);
        Stock stock = result.get();
        model.addAttribute("title", "Add Tag to: " + stock.getTicker());

        //model.addAttribute("tags", tagRepository.findAll());
        List<Tag> allTags = loggedInUser.getPortfolio().getTags();
        List<Tag> tags = new ArrayList<>();
        int alreadyTagged = 0;
        for (Tag tag : allTags) {
            if (!stock.getTags().contains(tag)) {
                tags.add(tag);
            } else {
                alreadyTagged++;
            }
        }
        int tagsSpotsLeft = MAX_NUMBER_OF_TAGS - alreadyTagged;

        if (tags.size() == 0) {//No tags in databse
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "warning|" +
                    NOT_TAG_MSG);
            return "redirect:/";
        }

        model.addAttribute("tags", tags);
        model.addAttribute(ACTION_MESSAGE_KEY, "info|Up to a maximum of " +
                MAX_NUMBER_OF_TAGS + " tags may be added to a stock." +
                " You may add " + tagsSpotsLeft + " more tag(s).");

        //model.addAttribute("stock", stock);
        StockTagDTO stockTagDTO = new StockTagDTO();
        stockTagDTO.setStock(stock);
        model.addAttribute("stockTag", stockTagDTO);
        return "stocks/add-tag";
    }

    /**
     * ONLY LOGGED IN
     * Add tag to a stock
     * responds to /stocks/add-tag?stockId=13
     *
     * @param stockTag
     * @param errors
     * @param redirectAttributes
     * @return
     */
    @PostMapping("add-tag")
    public String processAddTagForm(
            @ModelAttribute @Valid StockTagDTO stockTag,
            Errors errors,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        }

        if (!errors.hasErrors()) {
            Stock stock = stockTag.getStock();
            Tag tag = stockTag.getTag();
            if (!stock.getTags().contains(tag)) {
                stock.addTag(tag);
                stockRepository.save(stock);
                redirectAttributes.addFlashAttribute(ACTION_MESSAGE_KEY, "success|New investment field " + tag.getDisplayName() + " added to stock '" +
                        stock.getTicker() + "'");
            } else {
                redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "info|Investment field " + tag.getDisplayName() + " is already set to stock '" +
                        stock.getTicker() + "'");
            }
            return "redirect:detail/" + stock.getId();
        }
        return "redirect:add-tag/" + stockTag.getStock().getId();
    }

    /**
     * ONLY LOGGED IN
     * remove tag from a stock
     * responds to /stocks/remove-tag?stockId=13
     *
     * @param stockId
     * @param model
     * @return
     */
    @GetMapping("remove-tag/{stockId}")
    public String displayRemoveTagForm(
            @PathVariable Integer stockId,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        } else {//user is logged in
            model.addAttribute("isLoggedIn", true);
        }

        Optional<Stock> result = stockRepository.findById(stockId);
        Stock stock = result.get();
        model.addAttribute("title", "Remove Tag from: " + stock.getTicker());

        /*List<Tag> allTags = loggedInUser.getPortfolio().getTags();
        List<Tag> stockTags = stock.getTags();

        //intersection of the two lists
        Set<Tag> setOfTags = allTags.stream()
                .distinct()
                .filter(stockTags::contains)
                .collect(Collectors.toSet());*/
        //intersection of the lists of portfolio tags and all stock tags
        List<Tag> inPortfolioStockTags = stock.getInPortfolioTags(loggedInUser.getPortfolio().getTags());
        model.addAttribute("inPortfolioStockTags", inPortfolioStockTags);//tagRepository.findAll());
        model.addAttribute("stockId", stockId);
//        StockTagDTO stockTagDTO = new StockTagDTO();
//        stockTagDTO.setStock(stock);
//        model.addAttribute("stockTag", stockTagDTO);
        return "stocks/remove-tag";
    }

    /**
     * ONLY LOGGED IN
     * remove tag from a stock
     * responds to /stocks/remove-tag?stockId=13
     *
     * @param redirectAttributes
     * @return
     */
    @PostMapping("remove-tag")
    public String processRemoveTagForm(
            int stockId,
            @RequestParam(required = false) int[] tagIds,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        }

        Optional<Stock> result = stockRepository.findById(stockId);
        Stock stock = result.get();
        String messageString = "";
        if (tagIds != null) {
            for (int tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId).get();
                if (stock.getTags().contains(tag)) {
                    stock.getTags().remove(tag);
                    messageString += tag.getDisplayName() + " ";
                } // else {} No reason why that tag would not be in there.
            }
        }
        messageString = (messageString == "") ? "None" : messageString;
        redirectAttributes.addFlashAttribute(ACTION_MESSAGE_KEY, "success|The following investment(s) field(s) " +
                "got removed from stock " + stock.getTicker() + ": " + messageString);
        stockRepository.save(stock);
        return "redirect:detail/" + stock.getId();
    }

    /**
     * CALL USPTO API to refresh data
     */
    void loadUsptoAPI() {
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        globalSize = listOfStocks.size();
        //Preparatory settings For USPTO API call
        final String o = "{\"page\":0,\"per_page\":1}";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLL dd, yyyy");
        final String dateForUsptoApiUpdate = LocalDate.now().format(formatter);
        String usptoStockUrlString = "";
        String usptoApiQueryResult = "";
        String stock_q = ""; // contains the usptoApi key

        //for display of API update progress
        numberOfUsptoItemsDownloaded = 0;

        for (Stock stock : listOfStocks) {
            //GET USPTO Data while using usptoId
            usptoStockUrlString = BASE_URL_USPTO + "api/patents/query?q={stock_q}&f=[\"assignee_type\"]&o={o}";
            stock_q = "{\"assignee_id\":\"" + stock.getUsptoId() + "\"}";
            RestTemplate usptoRestTemplate = new RestTemplate();
            usptoApiQueryResult = usptoRestTemplate.getForObject(usptoStockUrlString, String.class, stock_q, o);

            // parsing usptoApiQueryResult
            try {
                Object usptoObj = new JSONParser().parse(usptoApiQueryResult);
                JSONObject usptoJo = (JSONObject) usptoObj; //typecasting usptoObj to JSONObject
                long total_patent_count = (long) usptoJo.get("total_patent_count");
                stock.getStockDetails().setTotalNumberOfPatents(total_patent_count);
                stock.getStockDetails().setLastUsptoApiUpdate(dateForUsptoApiUpdate);

                //Save USPTO API updates: lastUsptoApiUpdate, totalNumberOfPatents
                stock.getStockDetails().setTotalNumberOfPatents(total_patent_count);
                stock.getStockDetails().setLastUsptoApiUpdate(dateForUsptoApiUpdate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //Save obtained data to update local repositories
            stockRepository.save(stock);

            //for update of API download progress
            numberOfUsptoItemsDownloaded++;

            // System.out.println("stock " + numberOfUsptoItemsDownloaded + " --> " + stock.getStockDetails().getTotalNumberOfPatents());
        }
        //System.out.println("Completed USPTO API calls");
    }

    /**
     * This class represents the current number of
     * successfully called API data
     */
    class CurrentPercentageDownloaded {
        private long percentValue;

        public CurrentPercentageDownloaded(long percentValue) {
            this.percentValue = percentValue;
        }

        public long getPercentValue() {
            return percentValue;
        }

        public void setPercentValue(long percentValue) {
            this.percentValue = percentValue;
        }
    }

    /**
     * Resource that produces an application/json representation of a
     * progress bar percentage value to refresh progress bar for client side
     *
     * @return a JSON {@link CurrentPercentageDownloaded} object
     */
    @ResponseBody
    @GetMapping(path = "progress-bar-value", produces = MediaType.APPLICATION_JSON_VALUE)
    public CurrentPercentageDownloaded getProgressBarPercentageValue() {
//        System.out.println("PERCENTAGE1 = " + numberOfUsptoItemsDownloaded);
//        System.out.println("globalSize = " + globalSize);
//        System.out.println("PERCENTAGE2 = " + Math.round((double)numberOfUsptoItemsDownloaded*100 / (double)globalSize));
        return new CurrentPercentageDownloaded(Math.round((double) (numberOfUsptoItemsDownloaded + numberOfIexItemsDownloaded) * 100 / (double) (globalSize * 2)));
    }

    /**
     * CALL IEX API to refresh data
     */
    void loadIexApi() {
        //Preparatory settings For IEX API call
        String iexStockUrlString;
        String iexApiQueryResult = "";

        //BASE_URL_SANDBOX_IEX_SSE
        //IEX_SANDBOX_PUBLIC_TOKEN_TSK
        //BASE_URL_SANDBOX_IEX_SSE
        //IEX_SANDBOX_PUBLIC_TOKEN_TSK
        //IEX_SANDBOX_SECRET_TOKEN
//        backOff.setMaxInterval(5 * 1000L);
//        backOff.setMaxElapsedTime(50 * 1000L);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 1.1);
        backOff.setMaxElapsedTime(50000L);
        BackOffExecution execution = backOff.start();

        int i = 0; //for display of API update progress
        int k = 0;
        //int t = 0;
        int delta = 0;
        int oldDelta = 0;

        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();

        numberOfIexItemsDownloaded = 0;

        nextStock:
        for (Stock stock : listOfStocks) {
            k += 1;
            //System.out.println("Visiting stock " + k);
            //if (stock.getStockDetails().getLatestPrice() != 0.0) continue nextStock;
            //GET IEX Data while using ticker
            //https://sandbox.iexapis.com/stable/stock/twtr/quote?token=Tsk_acfbaf3e2b4444378ac1b46b2570b2da&filter=latestTime,latestPrice

            //Fake price data
            iexStockUrlString = BASE_URL_SANDBOX_IEX
                    + "/stable/stock/" + stock.getTicker() + "/quote?token="
                    + IEX_SANDBOX_PUBLIC_TOKEN_TSK + "&filter=latestTime,latestPrice";

            //Real price data
//            iexStockUrlString = BASE_URL_LIVE_IEX + "/stable/stock/" + stock.getTicker() + "/quote?token="
//                    + IEX_PUBLIC_TOKEN + "&filter=latestTime,latestPrice";

            RestTemplate iexRestTemplate = new RestTemplate();

            long waitTime = 0;

            //because too much time elapsed while trying
            stop:
            while (waitTime != BackOffExecution.STOP) {

                again:
                while (true) {
                    try {
                        iexApiQueryResult = iexRestTemplate.getForObject(iexStockUrlString, String.class);
                        //System.out.println("iexApiQueryResult = " + iexApiQueryResult);
                        if (iexApiQueryResult.charAt(0) == '{') {//read was successful
                            //System.out.println("iexApiQueryResult = " + iexApiQueryResult);
                            // parsing iexApiQueryResult
                            try {
                                Object iexObj = new JSONParser().parse(iexApiQueryResult);
                                JSONObject iexJo = (JSONObject) iexObj; //typecasting iexObj to JSONObject
                                String latestTime = (String) iexJo.get("latestTime");

                                double latestPrice = 0.0;
                                if (iexJo.get("latestPrice") instanceof Double) {
                                    //System.out.println("This is a Double");
                                    latestPrice = Double.valueOf((Double) iexJo.get("latestPrice"));
                                } else if (iexJo.get("latestPrice") instanceof String) {
                                    latestPrice = Double.valueOf((String) iexJo.get("latestPrice"));
                                    //System.out.println("This is a String");
                                } else if (iexJo.get("latestPrice") instanceof Float) {
                                    //System.out.println("This is a Float");
                                    latestPrice = Double.valueOf(((Float) iexJo.get("latestPrice")).floatValue());
                                } else if (iexJo.get("latestPrice") instanceof Long) {
                                    //System.out.println("This is a Long");
                                    latestPrice = Double.valueOf(((Long) iexJo.get("latestPrice")).longValue());
                                } else {
                                    latestPrice = Double.valueOf((String) iexJo.get("latestPrice"));
                                    System.out.println("1: latest price: " + iexJo.get("latestPrice")
                                            + " | This is a(n) " + iexJo.get("latestPrice").getClass().getSimpleName());
                                }
                                //latestPrice = Double.valueOf((String) iexJo.get("latestPrice"));
                                //System.out.println("latestTime " + latestTime + " | latestPrice " + latestPrice);

                                //Save IEX API updates: latestTime, latestPrice
                                stock.getStockDetails().setLastTradeTime(latestTime);
                                stock.getStockDetails().setLatestPrice(latestPrice);
                                //Save obtained data to update local repositories
                                stockRepository.save(stock);
                                numberOfIexItemsDownloaded++;
                                i++;

                                delta = k - i;
                                if (oldDelta != delta) {
                                    System.out.println("stock with k = " + k + " | i = " + i +
                                            " --> " + stock.getStockDetails().getLatestPrice());
                                    oldDelta = delta;
                                } else {

                                }

                                continue nextStock;
                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (NumberFormatException numberFormatException) {
                                numberFormatException.printStackTrace();
                            }
                        } else {
                            System.out.println("How come we are Here???");
                        }
                    } catch (Throwable ignored) {
                        waitTime = execution.nextBackOff();
                        if (waitTime != BackOffExecution.STOP) {
                            //System.out.println("Request just failed. Backing off for " + waitTime + "ms");
                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break again;
                        } else {
                            //t++;
//                            System.out.println("Not giving up on this stock " + k);
                            execution = backOff.start();
                            waitTime = 0;
                            break again;
                        }
                    }
                } //end while (true) again:

            }
        }
//        System.out.println("Read stocks i = " + i);
        //System.out.println("All stocks k = " + k);
//        System.out.println("Problematic stocks t = " + t);
        System.out.println("Completed IEX API calls");
    }

    /**
     * ONLY LOGGED IN
     *
     * @param redirectAttributes
     * @return
     */
    @GetMapping("callAPIs")
    public String displayStocksAfterCallingAPIs(
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser == null) {//user is NOT logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "danger|" +
                    NOT_LOGGED_IN_MSG);
            return "redirect:/";
        }

        loadUsptoAPI();
        loadIexApi();
        redirectAttributes.addFlashAttribute(ACTION_MESSAGE_KEY, "success|Completed API calls. You now have the latest market stock data.");
        //System.out.println("Completed API calls");
        return "redirect:/stocks";
    }

    /**
     * @param model
     * @param redirectAttributes
     * @return
     */
    @GetMapping("IP30")
    public String displayIP30(
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = authenticationController.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is logged in
            model.addAttribute("isLoggedIn", true);
        }

        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        int size = Math.min(30, listOfStocks.size());
        if (size < 30) {
            redirectAttributes.addFlashAttribute(ACTION_MESSAGE_KEY, "info|Not enough stocks in the database to make up IP30");
            redirectAttributes.addFlashAttribute("noIP30", "noIP30");
            return "redirect:/stocks";
        }
        listOfStocks.sort(Comparators.patentsPortfolioComparator.reversed());
        List<Stock> IP30List = new ArrayList<>(size);
        IP30List.addAll(listOfStocks.subList(0, size));
        model.addAttribute("title", "IP30");
        model.addAttribute("IP30List", IP30List);
        loadIP30PriceAndPatentsFootprint(model);
        model.addAttribute("exchangePlatforms", stockExchanges);
        return "stocks/IP30";
    }

    /**
     * Compute IP30's number of patents and
     * weighted price dynamically
     * Precondition : There are more than 30 stocks in the databse
     *
     * @param model
     */
    void loadIP30PriceAndPatentsFootprint(Model model) {
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        int size = 30;
        listOfStocks.sort(Comparators.patentsPortfolioComparator.reversed());
        List<Stock> IP30List = new ArrayList<>(size);
        IP30List.addAll(listOfStocks.subList(0, size));
        long aggregatedPatents = 0L;
        double numerator = 0.0;
        for (Stock stock : IP30List) {
            aggregatedPatents += stock.getStockDetails().getTotalNumberOfPatents();
            numerator += stock.getStockDetails().getTotalNumberOfPatents() * stock.getStockDetails().getLatestPrice();
        }
        double weightedPrice = numerator / aggregatedPatents;
        model.addAttribute("aggregatedPatents", aggregatedPatents);
        model.addAttribute("weightedPrice", weightedPrice);
    }

}
