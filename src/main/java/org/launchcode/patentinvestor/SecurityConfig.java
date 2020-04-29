package org.launchcode.patentinvestor;

/**
 * Created by kamdem
 */
//@Configuration
//@EnableWebSecurity
public class SecurityConfig {}//extends WebSecurityConfigurerAdapter {

    /*@Autowired
    DataSource dataSource;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }*/
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .authorizeRequests()
//                .regexMatchers("/register").permitAll()
//                .regexMatchers("/webjars/.*").permitAll()
//                .regexMatchers("/css/.*").permitAll()
//                .regexMatchers("/login?[^/]*").permitAll()
//                .anyRequest().authenticated()
//                .and()
//                .formLogin()
//                .loginPage("/login")
//                .usernameParameter("email")
//                .defaultSuccessUrl("/", true)
//                .permitAll()
//                .and()
//                .logout()
//                .logoutUrl("/logout")
//                .logoutSuccessUrl("/login?logout")
//                .and()
//                .rememberMe();
//    }

    /*@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authProvider());
        auth.userDetailsService(userDetailsService);
    }*/

//}
