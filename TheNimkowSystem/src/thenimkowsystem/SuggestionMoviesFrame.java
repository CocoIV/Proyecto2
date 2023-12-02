package thenimkowsystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * esta es la clase para la sugerencia de peliculas
 * @author metal
 */
public class SuggestionMoviesFrame extends JFrame {
    private JPanel contentPane;
    private MovieDetailsFrame movieDetailsFrame;

    private static final String API_KEY = "f846867b6184611eeff179631d3f9e26";
/**
 * metodo para sugerir las peliculas y crear el Jframe que las muestra
 * @param recommendedMovies
 * @param movieDetailsFrame 
 */
    public SuggestionMoviesFrame(List<String> recommendedMovies, MovieDetailsFrame movieDetailsFrame) {
        this.movieDetailsFrame = movieDetailsFrame;
        setTitle("Películas Recomendadas");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(contentPane);

        // Mostrar las películas recomendadas
        displayRecommendedMovies(recommendedMovies);

        add(scrollPane);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * metodo para mostrar las pelicula recomndadas 
     * @param recommendedMovies 
     */
    private void displayRecommendedMovies(List<String> recommendedMovies) {
        for (String movieTitle : recommendedMovies) {
            JPanel moviePanel = new JPanel();
            moviePanel.setLayout(new BorderLayout());
            moviePanel.setBackground(Color.LIGHT_GRAY);

            JsonObject movieDetails = fetchMovieDetailsByTitle(movieTitle);

            if (movieDetails != null) {
                try {
                    String posterPath = movieDetails.get("poster_path").getAsString();
                    URL posterURL = new URL("https://image.tmdb.org/t/p/w200" + posterPath);
                    ImageIcon posterIcon = new ImageIcon(posterURL);
                    JLabel posterLabel = new JLabel(posterIcon);

                    moviePanel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            String director = fetchDirector(movieDetails.get("id").getAsInt());
                            List<String> genres = fetchGenres(movieDetails.get("id").getAsInt());
                            String trailerLink = fetchTrailerLink(movieDetails.get("id").getAsInt());

                            // Mostrar la información en MovieDetailsFrame
                            new MovieDetailsFrame(movieTitle, movieDetails.get("overview").getAsString(), director,
                                    movieDetails.get("release_date").getAsString(), genres,
                                    fetchCast(movieDetails.get("id").getAsInt()), trailerLink);
                        }
                    });

                    moviePanel.add(posterLabel, BorderLayout.CENTER);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                contentPane.add(moviePanel);
                contentPane.add(Box.createRigidArea(new Dimension(0, 10))); // Espaciado vertical
            }
        }
    }
  
    /**
     * metodo fetch para obtner la informacion de la movie en el API
     * @param movieTitle
     * @return 
     */
    private JsonObject fetchMovieDetailsByTitle(String movieTitle) {
    try {
        // Codificar el título de la película para evitar problemas con caracteres especiales
        String encodedTitle = URLEncoder.encode(movieTitle, StandardCharsets.UTF_8);

        String urlStr = "https://api.themoviedb.org/3/search/movie?api_key=" + API_KEY + "&language=es-ES&page=1&query=" + encodedTitle;
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder response;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
        JsonArray resultsArray = jsonObject.getAsJsonArray("results");

        if (resultsArray.size() > 0) {
            return resultsArray.get(0).getAsJsonObject();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}

/**
 * metodo para obtener el director del API
 * @param movieId
 * @return 
 */
     private String fetchDirector(int movieId) {
        try {
            String urlStr = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + API_KEY;
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray crewArray = jsonObject.getAsJsonArray("crew");

            for (JsonElement element : crewArray) {
                JsonObject crewMember = element.getAsJsonObject();
                if (crewMember.get("job").getAsString().equals("Director")) {
                    return crewMember.get("name").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Desconocido";
    }

     /**
      * metodo fetch para obtener los generos del API
      * @param movieId
      * @return 
      */
    private List<String> fetchGenres(int movieId) {
        List<String> genresList = new ArrayList<>();
        try {
            String urlStr = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + API_KEY + "&language=es-ES";
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray genresArray = jsonObject.getAsJsonArray("genres");

            for (JsonElement element : genresArray) {
                JsonObject genre = element.getAsJsonObject();
                genresList.add(genre.get("name").getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return genresList;
    }

    /**
     * metodo fetch de el cast del API
     * @param movieId
     * @return 
     */
    private List<String> fetchCast(int movieId) {
        List<String> castList = new ArrayList<>();
        try {
            String urlStr = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + API_KEY;
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray castArray = jsonObject.getAsJsonArray("cast");

            for (JsonElement element : castArray) {
                JsonObject castMember = element.getAsJsonObject();
                castList.add(castMember.get("name").getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return castList;
    }

    /**
     * metodo fetch para obtener el trailer del API
     * @param movieId
     * @return 
     */
    private String fetchTrailerLink(int movieId) {
        try {
            String urlStr = "https://api.themoviedb.org/3/movie/" + movieId + "/videos?api_key=" + API_KEY;
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray videosArray = jsonObject.getAsJsonArray("results");

            for (JsonElement element : videosArray) {
                JsonObject video = element.getAsJsonObject();
                if (video.get("type").getAsString().equals("Trailer")) {
                    return "https://www.youtube.com/watch?v=" + video.get("key").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
   public static void main(String[] args) {
       
   }
}