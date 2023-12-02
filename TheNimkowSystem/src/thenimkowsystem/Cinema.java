
package thenimkowsystem;

/**
 *
 * @author Diego
 */
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * En la clase Cinema se genera un JFrame y extrae la informacion del API
 * y la acomoda de manera que se presente en el JFrame
 * @author Diego
 */

// Se declara de que numero de pagina inicia y la clave API
public class Cinema extends JFrame {
    private JPanel contentPane;
    private int currentPage = 1;
    private static final String API_KEY = "f846867b6184611eeff179631d3f9e26";
    
    /**
     * Aqui se declaran los diseños que tiene el JFrame pincipal en el que se 
     * ven todas las peliculas, botones de cambio de paginas y el buscador de
     * peliculas 
     */
    public Cinema() {
        setTitle("Movies from TMDb");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Se define el color negro para el fondo del JFrame
        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBackground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(contentPane);
        
        //Se definen el color del boton de busqueda que se usara en el JFrame
        JTextField searchField = new JTextField();
        searchField.setBackground(Color.DARK_GRAY);
        searchField.setForeground(Color.WHITE);
        JButton searchButton = new JButton("Buscar");
        searchButton.setBackground(Color.BLUE);
        searchButton.setForeground(Color.WHITE);

        searchButton.addActionListener(e -> {
            currentPage = 1;
            contentPane.removeAll();
            fetchMovies(searchField.getText().trim());
        });

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        searchPanel.setBackground(Color.BLACK);

        setLayout(new BorderLayout());
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        //Se define los botones para hacer el salto de pagina y su logica 
        JButton prevButton = new JButton("<< Página anterior");
        JButton nextButton = new JButton("Siguiente página >>");

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        paginationPanel.add(prevButton);
        paginationPanel.add(nextButton);
        add(paginationPanel, BorderLayout.SOUTH);
        
        //Logica de salto de pagina
        prevButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                contentPane.removeAll();
                fetchMovies(searchField.getText().trim());
            }
        });

        nextButton.addActionListener(e -> {
            currentPage++;
            contentPane.removeAll();
            fetchMovies(searchField.getText().trim());
        });

        fetchMovies("");
        setVisible(true);
    }
    
    /**
     * Presenta el trending de la pelicula y la busqueda de las peliculas
     * @param searchTerm 
     */
    private void fetchMovies(String searchTerm) {
        new Thread(() -> {
            try {
                String urlStr;
                if (!searchTerm.isEmpty()) {
                    urlStr = "https://api.themoviedb.org/3/search/movie?api_key="
                            + API_KEY +
                            "&language=es-ES&page=" + currentPage + "&query=" + 
                            searchTerm;
                } else {
                    urlStr = "https://api.themoviedb.org/3/trending/movie/week?api_key=" 
                            + API_KEY +
                            "&language=es-ES&page=" + currentPage;
                }

                URL url = new URL(urlStr);
                HttpURLConnection connection = 
                        (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                StringBuilder response;
                try (BufferedReader reader = new BufferedReader
        (new InputStreamReader(connection.getInputStream()))) {
                    response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JsonObject jsonObject = JsonParser.parseString(response.
                        toString()).getAsJsonObject();
                JsonArray results = jsonObject.getAsJsonArray("results");

                for (int i = 0; i < results.size(); i++) {
                    JsonObject movie = results.get(i).getAsJsonObject();

                    int movieId = movie.get("id").getAsInt();
                    List<String> genres = fetchGenres(movieId);
                    if (!genres.isEmpty()) {
                        int genreId = fetchGenreId(genres.get(0));
                        List<String> recommendedMovies = 
                                fetchRecommendedMoviesByGenre(genreId);
                        displayMovieInfo(movie, recommendedMovies);
                    } else {
                        displayMovieInfo(movie, new ArrayList<>());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * En esta clase se hace la extraccion de los datos detallados de la 
     * pelicula 
     * @param movie
     * @param recommendedMovies 
     */
    private void displayMovieInfo(JsonObject movie, List<String> 
            recommendedMovies) {
        String title = movie.get("title").getAsString();
        double voteAverage = movie.get("vote_average").getAsDouble();
        String posterPath = movie.get("poster_path").getAsString();

        JPanel moviePanel = new JPanel();
        moviePanel.setLayout(new BoxLayout(moviePanel, BoxLayout.Y_AXIS));
        moviePanel.setBackground(Color.DARK_GRAY);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Gotham", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        moviePanel.add(titleLabel);

        JLabel voteLabel = new JLabel("Votación: " + voteAverage);
        voteLabel.setForeground(Color.WHITE);
        moviePanel.add(voteLabel);

        try {// se hace la extraccion de las imagenes de las peliculas 
            URL posterURL = new URL("https://image.tmdb.org/t/p/w200" + 
                    posterPath);
            ImageIcon posterIcon = new ImageIcon(posterURL);
            JLabel posterLabel = new JLabel(posterIcon);

            moviePanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int movieId = movie.get("id").getAsInt();
                    String trailerLink = fetchTrailerLink(movieId);

                    // Se crea una instancia de MovieDetailsFrame
                    MovieDetailsFrame movieDetailsFrame = new MovieDetailsFrame
        (title, "", "", "", new ArrayList<>(), new ArrayList<>(), trailerLink);

                    // Se verifica si hay películas recomendadas y
                    //se abre la ventana correspondiente
                    if (recommendedMovies != null && !recommendedMovies.
                            isEmpty()) {
                        new SuggestionMoviesFrame(recommendedMovies, 
                                movieDetailsFrame);
                    }
                }
            });

            moviePanel.add(posterLabel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        contentPane.add(moviePanel);
        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPane.revalidate();
        contentPane.repaint();
    }
    
    /**
     * Se hace la extraccion de la informacion del genero o categoria de la 
     * pelicula 
     * @param movieId
     * @return 
     */
    private List<String> fetchGenres(int movieId) {
        try {
            String urlStr = "https://api.themoviedb.org/3/movie/"
                    + movieId + "?api_key=" + API_KEY + "&language=es-ES";
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) 
                    url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader
        (new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject jsonObject = JsonParser.parseString(response.toString
        ()).getAsJsonObject();
            JsonArray genresArray = jsonObject.getAsJsonArray("genres");

            List<String> genres = new ArrayList<>();
            for (JsonElement element : genresArray) {
                genres.add(element.getAsJsonObject().get("name").getAsString());
            }

            return genres;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    private int fetchGenreId(String genreName) {
        try {
            String urlStr = 
                    "https://api.themoviedb.org/3/genre/movie/list?api_key=" 
                    + API_KEY + "&language=es-ES";
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection)
                    url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader
        (new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // Se extrae por genero, nombre e ID de la pelicula 
            JsonObject jsonObject = JsonParser.parseString(response.toString())
                    .getAsJsonObject();
            JsonArray genresArray = jsonObject.getAsJsonArray("genres");

            for (JsonElement element : genresArray) {
                JsonObject genre = element.getAsJsonObject();
                if (genre.get("name").getAsString().
                        equalsIgnoreCase(genreName)) {
                    return genre.get("id").getAsInt();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
    
    
    /**
     * En esta clase se hace la extraccion de informacion de las peliculas
     * recomendadas 
     * @param genreId
     * @return 
     */
    private List<String> fetchRecommendedMoviesByGenre(int genreId) {
        try {
            String urlStr = 
                    "https://api.themoviedb.org/3/discover/movie?api_key=" 
                    + API_KEY +
                    "&language=es-ES&page=1&with_genres=" + genreId + 
                    "&sort_by=popularity.desc";

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.
                    openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader
        (new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject discoverObject = JsonParser.parseString(response.
                    toString()).getAsJsonObject();
            JsonArray results = discoverObject.getAsJsonArray("results");

            List<String> recommendedMovies = new ArrayList<>();
            for (JsonElement element : results) {
                JsonObject movie = element.getAsJsonObject();
                String title = movie.get("title").getAsString();
                recommendedMovies.add(title);
            }

            return recommendedMovies;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
    
    /**
     * En esta clase se hace la extraccion de los enlaces para las peliculas  
     * @param movieId
     * @return 
     */
    private String fetchTrailerLink(int movieId) {
        try {
            String urlStr = 
                    "https://api.themoviedb.org/3/movie/" + movieId +
                    "/videos?api_key=" + API_KEY;
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection)
                    url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader
        (new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject jsonObject = JsonParser.parseString(response.toString())
                    .getAsJsonObject();
            JsonArray videosArray = jsonObject.getAsJsonArray("results");

            for (JsonElement element : videosArray) {
                JsonObject video = element.getAsJsonObject();
                if (video.get("type").getAsString().equals("Trailer")) {
                    return "https://www.youtube.com/watch?v=" + video.get("key")
                            .getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Cinema::new);
    }
}