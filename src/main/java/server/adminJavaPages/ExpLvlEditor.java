package server.adminJavaPages;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import controllers.dao.CreepyGuyDAO;
import controllers.dao.LoginAccesDAO;
import models.CreepyGuyModel;
import models.Level;
import models.Room;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import server.helpers.CookieHelper;
import server.helpers.FormDataParser;

public class ExpLvlEditor implements HttpHandler {

    private CreepyGuyDAO creepyGuyDAO;
    private Optional<HttpCookie> cookie;
    private CookieHelper cookieHelper;
    private LoginAccesDAO loginAccesDAO;
    private FormDataParser formDataParser;
    private CreepyGuyModel creepyGuyModel;
    private String expLevelId;

    public ExpLvlEditor(Connection connection){
        creepyGuyDAO = new CreepyGuyDAO(connection);
        formDataParser = new FormDataParser();
        cookieHelper = new CookieHelper();
        loginAccesDAO = new LoginAccesDAO(connection);

    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String response = "";
        cookie = cookieHelper.getSessionIdCookie(httpExchange);
        String sessionId = cookie.get().getValue().substring(1, cookie.get().getValue().length() - 1);
        String method = httpExchange.getRequestMethod();
        creepyGuyModel = creepyGuyDAO.getAdminBySessionId(sessionId);

        if (cookie.isPresent()) {
            if (loginAccesDAO.checkSessionPresent(sessionId)){

                if (method.equals("GET")) {
                    response = generatePage();
                }

                if (method.equals("POST")){
                    Map inputs = formDataParser.getData(httpExchange);
                    if (inputs.containsKey("search")){
                        expLevelId = inputs.get("ID").toString();
                        response = fillPage(expLevelId);
                    }
                    if (inputs.containsKey("edit")){
                        Map <String, String> lvlData = new HashMap<>();
                        lvlData.put("roomName", inputs.get("name").toString());
                        lvlData.put("roomDescription", inputs.get("description").toString());
                        creepyGuyDAO.editLevel(new Level(lvlData), expLevelId);
                        response = generatePage();
                    }
                    if (inputs.containsKey("delete")){
                        creepyGuyDAO.deleteLevel(expLevelId);
                        response = generatePage();
                    }
                    if (inputs.containsKey("add")){
                        inputs = formDataParser.getData(httpExchange);
                        Map <String, String> levelData = new HashMap<>();
                        levelData.put("roomName", inputs.get("name").toString());
                        levelData.put("roomDescription", inputs.get("description").toString());
                        creepyGuyDAO.addLevel(new Level(levelData));
                        response = generatePage();
                    }

                }

            }
            else{
                httpExchange.getResponseHeaders().set("Location", "/login");
            }
        }
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

    private String generatePage(){


        JtwigTemplate template = JtwigTemplate.classpathTemplate("HTML/adminPages/expLvlEditor.twig");


        JtwigModel model = JtwigModel.newModel();

        model.with("nickname", creepyGuyModel.getNickName());

        return template.render(model);
    }

    private String fillPage(String id){
        Level level = creepyGuyDAO.getLevelById(id);
        JtwigTemplate template = JtwigTemplate.classpathTemplate("HTML/adminPages/expLvlEditor.twig");
        JtwigModel model = JtwigModel.newModel();

        model.with("nickname", creepyGuyModel.getNickName());
        model.with("description", level.getThreshold());
        model.with("name", level.getLevelName());


        return template.render(model);
    }
}