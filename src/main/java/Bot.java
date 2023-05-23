import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.sql.*;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    String login = "";
    String password = "";
    String adminlog = "";
    String adminpsd = "";
    String dat = "";
    String price = "";
    int statebooking = 0;
    static List<Service> services = new ArrayList<>();
    static List<Datatime> datatimes = new ArrayList<>();
    public ArrayList<String> arr = new ArrayList<>();
    long chatId;
    public static void main(String[] args) throws TelegramApiException, SQLException {

        Connection connection = null;
        connection = DriverManager.getConnection("jdbc:sqlite:C://Users//dragu//IdeaProjects//CarService//new.db");
        if (connection != null) {
            System.out.println("Соединение установлено!");
        }
        String query = "SELECT id_serv, info, price, title FROM Services";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            String id = resultSet.getString("id_serv");
            String info = resultSet.getString("info");
            String price = resultSet.getString("price");
            String title = resultSet.getString("title");
            Service service = new Service(id, info, price, title);
            services.add(service);
        }
        String query2 = "SELECT id, info_time FROM Datatime";
        ResultSet resultSet2 = statement.executeQuery(query2);
        while (resultSet2.next()) {
            String id = resultSet2.getString("id");
            String info_time = resultSet2.getString("info_time");
            Datatime datatime = new Datatime(id, info_time);
            datatimes.add(datatime);
        }
        resultSet.close();
        statement.close();

        Bot telegramBot = new Bot();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(telegramBot);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            try {
                processCallbackQuery(update.getCallbackQuery());

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

        }

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
            Connection connection = null;
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:C://Users//dragu//IdeaProjects//CarService//new.db");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try {
                handleMessahe(update.getMessage());
                String messageText = update.getMessage().getText();
                arr.add(messageText);
                System.out.println("arr = " + arr);

                if (arr.get(0).equals("/signup") && arr.size() == 3) {
                    PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM user WHERE login = ?");
                    checkStatement.setString(1, arr.get(1));
                    ResultSet resultSet = checkStatement.executeQuery();
                    resultSet.next();
                    int count = resultSet.getInt(1);
                    if (count > 0) {
                        execute(SendMessage.builder().chatId(chatId).text("Ошибка: логин уже существует, попробуйте еще раз!").build());
                        arr.clear();
                        arr.add("/signup");
                    } else {
                        checkStatement.close();
                        resultSet.close();
                        execute(SendMessage.builder().chatId(chatId).text("Регистрация прошла успешно!").build());
                        addUser(arr.get(1), arr.get(2));
                    }
                }

                if (arr.get(0).equals("/signin")) {
                    int count;
                    if (arr.get(1).equals("admin")) {
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM Mechanic WHERE password = ?");
                        checkStatement.setString(1, arr.get(2));
                        ResultSet resultSet = checkStatement.executeQuery();
                        resultSet.next();
                        count = resultSet.getInt(1);
                        if (count == 1) {
                            adminlog = arr.get(1);
                            adminpsd = arr.get(2);
                            execute(SendMessage.builder().chatId(chatId).text("Вход для механика выполнен успешно!").build());
                            arr.clear();
                            checkStatement.close();
                            resultSet.close();
                        } else {
                            execute(SendMessage.builder().chatId(chatId).text("Неверные данные! Попробуйте еще раз").build());
                            arr.clear();
                            arr.add("/signin");

                        }
                    } else {
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM user WHERE (login, password) = (?,?)");
                        checkStatement.setString(1, arr.get(1));
                        checkStatement.setString(2, arr.get(2));
                        ResultSet resultSet = checkStatement.executeQuery();
                        resultSet.next();
                        count = resultSet.getInt(1);
                        if (count == 1) {
                            execute(SendMessage.builder().chatId(chatId).text("Вход выполнен успешно!").build());
                            login = arr.get(1);
                            password = arr.get(2);
                            arr.clear();
                            checkStatement.close();
                            resultSet.close();
                        } else {
                            execute(SendMessage.builder().chatId(chatId).text("Неверные данные! Попробуйте еще раз").build());
                            arr.clear();
                            arr.add("/signin");
                        }
                    }
                }

                if (arr.get(0).equals("/services")) {
                    execute(SendMessage.builder().chatId(chatId).text("Доступные услуги:").build());
                    StringBuilder message = new StringBuilder();
                    for (Service service : services) {
                        message.append(service.getId()).append(")");
                        message.append("Название: ").append(service.getTitle()).append("\n");
                        message.append("Информация: ").append(service.getInfo()).append("\n");
                        message.append("Цена: ").append(service.getPrice()).append("\n\n");
                    }
                    execute(SendMessage.builder().chatId(chatId).text(message.toString()).build());
                    arr.clear();
                }

                if (arr.get(0).equals("/booking")) {
                    if (messageText.equals("выход")) {
                        execute(SendMessage.builder().chatId(chatId).text("Бронирование прервано! ").build());
                        arr.clear();
                    }
                    if (login.equals("") || password.equals("")) {
                        execute(SendMessage.builder().chatId(chatId).text("Ошибка: для использования функции вы должны войти в аккаунт клиента!").build());
                        arr.clear();
                    }
                    if (statebooking == 0) {
                        boolean tmp = false;
                        for (Service service : services) {
                            if (service.getTitle().equals(arr.get(1))) {
                                tmp = true;
                                price = service.getPrice();
                            }
                        }
                        if (tmp) {
                            execute(SendMessage.builder().chatId(chatId).text("Введите номер даты услуги (день-часы-минуты)").build());
                            StringBuilder message = new StringBuilder();
                            for (Datatime datatime : datatimes) {
                                message.append(datatime.getId()).append(") ");
                                message.append(datatime.getInfo()).append("\n\n");
                            }
                            execute(SendMessage.builder().chatId(chatId).text(message.toString()).build());
                            statebooking++;
                        } else {
                            execute(SendMessage.builder().chatId(chatId).text("Такой услуги не существует! Попробуйте еще раз. ").build());
                            arr.remove(1);
                        }
                    }

                    if (statebooking == 1) {
                        boolean tmp = false;
                        for (Datatime datatime : datatimes) {
                            if (datatime.getId().equals(arr.get(2))) {
                                tmp = true;
                                dat = datatime.getInfo();
                            }
                        }

                        if (tmp) {
                            String b = "DELETE FROM Datatime WHERE id = " + arr.get(2);
                            Statement statement2 = connection.createStatement();
                            int rowsDeleted = statement2.executeUpdate(b);
                            statebooking++;
                        } else {
                            execute(SendMessage.builder().chatId(chatId).text("Нет даты с таким номером! Попробуйте еще раз. ").build());
                            arr.remove(2);
                        }
                    }
                    if (statebooking == 2) {
                        execute(SendMessage.builder().chatId(chatId).text("Введите ваше имя: ").build());
                        statebooking++;
                    }
                    if (statebooking == 3) {
                        StringBuilder message = new StringBuilder();
                        message.append("Ваш заказ: ").append("\n\n");
                        message.append("Услуга: ").append(arr.get(1)).append("\n");
                        message.append("Дата: ").append(dat).append("\n");
                        message.append("Стоимость: ").append(price).append("\n");
                        message.append("Заказ на имя: ").append(arr.get(3)).append("\n\n");
                        message.append("Для подтверждения брони выберете да/нет");
                        execute(SendMessage.builder().chatId(chatId).text(message.toString()).build());
                        ArrayList<InlineKeyboardButton> buttons = new ArrayList<>();
                        buttons.add(InlineKeyboardButton.builder().text("да").callbackData("1").build());
                        buttons.add(InlineKeyboardButton.builder().text("нет").callbackData("2").build());
                        execute(SendMessage.builder().text("Выберете вариант").chatId(chatId).replyMarkup(InlineKeyboardMarkup.builder().keyboard(Collections.singleton(buttons)).build()).build());
                    }
                }

                if (arr.get(0).equals("/mybooking")){
                    if (login.equals("") || password.equals("")) {
                        execute(SendMessage.builder().chatId(chatId).text("Ошибка: для использования функции вы должны войти в аккаунт клиента!").build());
                        arr.clear();
                    }
                    if(statebooking == 0 && adminlog.equals("")){
                        StringBuilder message = new StringBuilder();
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT id, name, date_post, price, payment FROM Orderlist where id_user = ?");
                        checkStatement.setString(1, login);
                        ResultSet resultSet3 = checkStatement.executeQuery();

                        while (resultSet3.next()) {
                            String id = resultSet3.getString("id");
                            String name = resultSet3.getString("name");
                            String date_post = resultSet3.getString("date_post");
                            String price = resultSet3.getString("price");
                            String payment = resultSet3.getString("payment");
                            message.append(id).append(") ");
                            message.append("Имя: ").append(name).append("\n");
                            message.append("Дата: ").append(date_post).append("\n");
                            message.append("Цена: ").append(price).append("\n");
                            message.append("Способ оплаты: ").append(payment).append("\n\n");
                        }
                        if (message.isEmpty()){
                            execute(SendMessage.builder().chatId(chatId).text("Ваш список пуст\n\nДля создания записи напишите /booking ").build());
                            resultSet3.close();
                            checkStatement.close();
                        }
                        else {
                            execute(SendMessage.builder().chatId(chatId).text(message.toString()).build());
                            resultSet3.close();
                            checkStatement.close();
                            ArrayList<InlineKeyboardButton> buttons = new ArrayList<>();
                            buttons.add(InlineKeyboardButton.builder().text("Удалить запись").callbackData("6").build());
                            execute(SendMessage.builder().text("Для удаления записи из вашего списка нажмите на кнопку.").chatId(chatId).replyMarkup(InlineKeyboardMarkup.builder().keyboard(Collections.singleton(buttons)).build()).build());
                        }
                    }
                    if (statebooking == 1){
                        int count;
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM Orderlist WHERE id = "+messageText);
                        ResultSet resultSet = checkStatement.executeQuery();
                        resultSet.next();
                        count = resultSet.getInt(1);
                        checkStatement.close();
                        resultSet.close();
                        if (count == 1){
                            String b = "DELETE FROM Orderlist WHERE id = " + messageText;
                            Statement statement2 = connection.createStatement();
                            int rowsDeleted = statement2.executeUpdate(b);
                            execute(SendMessage.builder().chatId(chatId).text("Запись удалена!").build());
                            statement2.close();
                            statebooking = 2;
                        }
                        else {
                            execute(SendMessage.builder().chatId(chatId).text("Неверные данные! Попробуйте еще раз").build());
                            arr.clear();
                            arr.add("/mybooking");
                        }
                    }
                }

                if (arr.get(0).equals("/allbooking")){
                    if (adminlog.equals("") || adminpsd.equals("")) {
                        execute(SendMessage.builder().chatId(chatId).text("Ошибка: у вас нет доступа к этой функции!").build());
                        arr.clear();
                    }else statebooking++;

                    if(statebooking == 1){
                        StringBuilder message = new StringBuilder();
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT id, name, date_post, price, payment FROM Orderlist");
                        ResultSet resultSet3 = checkStatement.executeQuery();
                        while (resultSet3.next()) {
                            String id = resultSet3.getString("id");
                            String name = resultSet3.getString("name");
                            String date_post = resultSet3.getString("date_post");
                            String price = resultSet3.getString("price");
                            String payment = resultSet3.getString("payment");
                            message.append(id).append(") ");
                            message.append("Имя: ").append(name).append("\n");
                            message.append("Дата: ").append(date_post).append("\n");
                            message.append("Цена: ").append(price).append("\n");
                            message.append("Способ оплаты: ").append(payment).append("\n\n");
                        }

                        if (message.isEmpty()){
                            execute(SendMessage.builder().chatId(chatId).text("Список записей пуст!").build());
                            resultSet3.close();
                            checkStatement.close();
                        }
                        else {
                            execute(SendMessage.builder().chatId(chatId).text(message.toString()).build());
                            resultSet3.close();
                            checkStatement.close();
                            ArrayList<InlineKeyboardButton> buttons = new ArrayList<>();
                            buttons.add(InlineKeyboardButton.builder().text("Удалить запись").callbackData("5").build());
                            execute(SendMessage.builder().text("Для удаления записи из списка нажмите на кнопку.").chatId(chatId).replyMarkup(InlineKeyboardMarkup.builder().keyboard(Collections.singleton(buttons)).build()).build());

                        }
                    }
                    if (statebooking == 3){
                        int count;
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM Orderlist WHERE id = "+messageText);
                        ResultSet resultSet = checkStatement.executeQuery();
                        resultSet.next();
                        count = resultSet.getInt(1);
                        checkStatement.close();
                        resultSet.close();
                        if (count == 1){
                            String b = "DELETE FROM Orderlist WHERE id = " + messageText;
                            Statement statement2 = connection.createStatement();
                            int rowsDeleted = statement2.executeUpdate(b);
                            execute(SendMessage.builder().chatId(chatId).text("Запись удалена!").build());
                            statement2.close();
                        }
                        else {
                            execute(SendMessage.builder().chatId(chatId).text("Неверные данные! Попробуйте еще раз").build());
                            arr.clear();
                            arr.add("/allbooking");
                        }
                    }
                }

                if (arr.get(0).equals("/change")){
                    if (adminlog.equals("") || adminpsd.equals("")) {
                        execute(SendMessage.builder().chatId(chatId).text("Ошибка: у вас нет доступа к этой функции!").build());
                        arr.clear();
                    }else statebooking++;

                    if (statebooking == 1){
                        ArrayList<InlineKeyboardButton> buttons = new ArrayList<>();
                        buttons.add(InlineKeyboardButton.builder().text("Удалить услугу").callbackData("7").build());
                        buttons.add(InlineKeyboardButton.builder().text("Добавить услугу").callbackData("8").build());
                        execute(SendMessage.builder().text("Для изменения предоставляемых услуг нажмите на кнопку.").chatId(chatId).replyMarkup(InlineKeyboardMarkup.builder().keyboard(Collections.singleton(buttons)).build()).build());
                    }
                    if (statebooking == 3){
                        int count;
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM Services WHERE id_serv = "+arr.get(1));
                        ResultSet resultSet = checkStatement.executeQuery();
                        resultSet.next();
                        count = resultSet.getInt(1);
                        checkStatement.close();
                        resultSet.close();
                        System.out.println(count);
                        if (count == 1){
                            String b = "DELETE FROM Services WHERE id_serv = " + messageText;
                            Statement statement2 = connection.createStatement();
                            int rowsDeleted = statement2.executeUpdate(b);
                            execute(SendMessage.builder().chatId(chatId).text("Услуга удалена!").build());
                            statement2.close();
                            services.clear();
                            String query = "SELECT id_serv, info, price, title FROM Services";
                            Statement statement3 = connection.createStatement();
                            ResultSet resultSet2 = statement3.executeQuery(query);
                            while (resultSet2.next()) {
                                String id = resultSet2.getString("id_serv");
                                String info2 = resultSet2.getString("info");
                                String price2 = resultSet2.getString("price");
                                String title2 = resultSet2.getString("title");
                                Service service = new Service(id, info2, price2, title2);
                                services.add(service);
                            }
                            statement3.close();
                            resultSet2.close();
                            arr.clear();
                        } else {
                            execute(SendMessage.builder().chatId(chatId).text("Неверные данные! Попробуйте еще раз").build());
                            arr.clear();
                            arr.add("/change");
                            statebooking = 2;
                        }
                    }
                    if (statebooking == 4){
                        execute(SendMessage.builder().chatId(chatId).text("Напишите цену услуги").build());
                    }
                    if (statebooking == 5){
                        execute(SendMessage.builder().chatId(chatId).text("Напишите описание услуги").build());
                    }
                    if (statebooking == 6){
                        addServ(arr.get(1),arr.get(2),arr.get(3));
                        execute(SendMessage.builder().chatId(chatId).text("Услуга добавлена!").build());
                        arr.clear();
                    }
                }

                if(arr.get(0).equals("/add")){
                    if (adminlog.equals("") || adminpsd.equals("")) {
                        execute(SendMessage.builder().chatId(chatId).text("Ошибка: у вас нет доступа к этой функции!").build());
                        arr.clear();
                    }else {
                        if (statebooking == 0) {
                            try {
                            int day = Integer.parseInt(arr.get(1));
                            if (day < 1 || day > 30){
                                execute(SendMessage.builder().chatId(chatId).text("Неправильные данные, попробуйте еще раз!").build());
                                arr.remove(1);
                            }else {
                                statebooking++;
                                execute(SendMessage.builder().chatId(chatId).text("Напишите час для новой записи").build());
                            }
                            } catch (NumberFormatException e) {
                                execute(SendMessage.builder().chatId(chatId).text("Ошибка: введенное значение должно быть числом, попробуйте еще раз!").build());
                                arr.remove(1);
                            }
                        }
                        if (statebooking == 1) {
                            try {
                                int hour = Integer.parseInt(arr.get(2));
                                if (hour < 10 || hour > 21){
                                    execute(SendMessage.builder().chatId(chatId).text("Неправильные данные, попробуйте еще раз!").build());
                                    arr.remove(2);
                                }else {
                                    execute(SendMessage.builder().chatId(chatId).text("Напишите минуты для новой записи").build());
                                    statebooking++;
                                }
                            } catch (NumberFormatException e) {
                                execute(SendMessage.builder().chatId(chatId).text("Ошибка: введенное значение должно быть числом, попробуйте еще раз!").build());
                                arr.remove(2);
                            }
                        }
                        if (statebooking == 2) {
                            try {
                                int min = Integer.parseInt(arr.get(3));
                                if (min == 0 || min == 30){
                                    execute(SendMessage.builder().chatId(chatId).text("Время для записи создано!").build());
                                    statebooking++;
                                }else {
                                    execute(SendMessage.builder().chatId(chatId).text("Неправильные данные, попробуйте еще раз!").build());
                                    arr.remove(3);
                                }
                            } catch (NumberFormatException e) {
                                execute(SendMessage.builder().chatId(chatId).text("Ошибка: введенное значение должно быть числом, попробуйте еще раз!").build());
                                arr.remove(3);
                            }
                        }
                        if(statebooking == 3){
                            int id_count = 1;
                            Statement stmt = connection.createStatement();
                            ResultSet rs = stmt.executeQuery("SELECT id FROM Datatime ORDER BY id ASC");
                            while(rs.next()) {
                                int id = rs.getInt("id");
                                if (id != id_count) {
                                    break;
                                }
                                id_count++;
                            }
                            stmt.close();
                            rs.close();

                            addTime(arr.get(1),arr.get(2),arr.get(3),id_count);
                            arr.clear();
                        }
                    }
                }
            } catch (TelegramApiException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void processCallbackQuery(CallbackQuery buttonQuery) throws TelegramApiException {
        if (buttonQuery.getData().equals("1")){
            ArrayList<InlineKeyboardButton> buttons = new ArrayList<>();
            buttons.add(InlineKeyboardButton.builder().text("Наличными").callbackData("3").build());
            buttons.add(InlineKeyboardButton.builder().text("Онлайн").callbackData("4").build());
            execute(SendMessage.builder().text("Какой способ оплаты?").chatId(chatId).replyMarkup(InlineKeyboardMarkup.builder().keyboard(Collections.singleton(buttons)).build()).build());
            for (Datatime datatime : datatimes) {
                if (datatime.getId().equals(arr.get(2))) {
                    datatimes.remove(datatime);
                    statebooking++;
                }
            }
        }
        if (buttonQuery.getData().equals("2")){
            execute(SendMessage.builder().chatId(chatId).text("Бронирование прервано!").build());
            arr.clear();
        }
        if (buttonQuery.getData().equals("3")){
            execute(SendMessage.builder().chatId(chatId).text("Бронь создана, вы можете просмотреть ее в своих записях.\n\nПосле завершения работ и выставления счета, оплатите его наличными, предоставив сумму соответствующую общей стоимости услуг.").build());
            addOrder(login,arr.get(3),dat,price,"Оплата наличными");
        }
        if (buttonQuery.getData().equals("4")){
            execute(SendMessage.builder().chatId(chatId).text("Бронь создана, вы можете просмотреть ее в своих записях.\n\nПосле ремонта механика, вам будет необходимо перевести сумму на указанный счет *xxx-xxx-xxx* и предъявить подтверждение операции.").build());
            addOrder(login,arr.get(3),dat,price,"Оплата онлайн");
        }
        if (buttonQuery.getData().equals("5")){
            execute(SendMessage.builder().chatId(chatId).text("Запись под каким номером вы хотите удалить?").build());
            statebooking = 2;
        }
        if (buttonQuery.getData().equals("6")){
            execute(SendMessage.builder().chatId(chatId).text("Запись под каким номером вы хотите удалить?").build());
            statebooking = 1;
        }
        if (buttonQuery.getData().equals("7")){
            execute(SendMessage.builder().chatId(chatId).text("Услугу под каким номером вы хотите удалить?").build());
            statebooking = 2;
            arr.clear();
            arr.add("/change");
        }
        if (buttonQuery.getData().equals("8")){
            execute(SendMessage.builder().chatId(chatId).text("Напишите название услуги").build());
            arr.clear();
            arr.add("/change");
            statebooking = 3;
        }
    }

    public void handleMessahe(Message message) throws TelegramApiException {
        long chatId = message.getChatId();

        if (message.hasText() && message.hasEntities()) {
            Optional<MessageEntity> commandEntity = message.getEntities().stream().filter(e -> "bot_command".equals(e.getType())).findFirst();
            if (commandEntity.isPresent()) {
                String command = message.getText().substring(commandEntity.get().getOffset(), commandEntity.get().getLength());
                if (command.equals("/signup")) {
                    execute(SendMessage.builder().chatId(chatId).text("Первым сообщением отправьте логин, вторым пароль").build());
                    login = "";
                    password = "";
                    adminlog = "";
                    adminpsd = "";
                    arr.clear();
                }
                if (command.equals("/signin")) {
                    execute(SendMessage.builder().chatId(chatId).text("Первым сообщением отправьте логин, вторым пароль").build());
                    login = "";
                    password = "";
                    adminlog = "";
                    adminpsd = "";
                    arr.clear();
                }
                if (command.equals("/mybooking")) {
                    arr.clear();
                    statebooking = 0;
                }
                if (command.equals("/services")) {
                    arr.clear();
                }
                if (command.equals("/booking")) {
                    arr.clear();
                    statebooking = 0;
                    if (!login.equals("")){
                        execute(SendMessage.builder().chatId(chatId).text("Для завершения создания записи напишите *выход*\n\nВведите название услуги.").build());
                    }
                }
                if (command.equals("/allbooking")) {
                    statebooking = 0;
                    arr.clear();
                }
                if (command.equals("/change")) {
                    statebooking = 0;
                    arr.clear();
                }
                if (command.equals("/help")){
                    execute(SendMessage.builder().chatId(chatId).text("Добро пожаловать в телеграмм бот Car Service System! Этот бот разработан специально для автосервиса Авто-Планета. Если у вас возникли трудности или вам нужна помощь, обратитесь к нам и мы с радостью вам поможем.\n" +
                            "\n" +
                            "Для начала работы с ботом вы можете создать свой аккаунт. Для этого необходимо написать команду /signup и следовать инструкциям." +
                            "После успешной регистрации вы сможете создавать записи в автосервисе.\n" +
                            "\n" +
                            "Чтобы создать запись, напишите команду /booking и заполните необходимые поля. Пожалуйста, убедитесь, что вы вводите корректные данные, чтобы избежать ошибок.\n\n" +
                            "Вы также можете просмотреть все свои записи, написав команду /mybooking Здесь вы найдете полную информацию о своих записях, включая дату и время, услуги и стоимость.\n" +
                            "\n" +
                            "Если вы хотите ознакомиться с предоставляемыми услугами, напишите команду /services. Здесь вы найдете подробную информацию о том, какие услуги предоставляет наш автосервис.\n\n" +
                            "Спасибо, что выбрали Car Service System! Если у вас есть какие-либо вопросы или проблемы, не стесняйтесь обращаться к нам. Мы всегда готовы помочь вам.\n" ).build());
                }
                if (command.equals("/add")){
                    execute(SendMessage.builder().chatId(chatId).text("Напишите день для новой записи").build());
                    statebooking = 0;
                    arr.clear();
                }
            }
        }
    }

    public static void addOrder(String login, String name, String date_post,String price,String payment){
        try {
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:sqlite:C://Users//dragu//IdeaProjects//CarService//new.db");

            String query2 = "INSERT INTO Orderlist (id_user,name,date_post,price,payment) " +
                    "VALUES ('" + login + "' , '" + name + "' , '" + date_post + "', '" + price + "', '" + payment + "')";

            Statement statement = connection.createStatement();
            statement.executeUpdate(query2);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void addUser(String login, String password) {
        try {
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:sqlite:C://Users//dragu//IdeaProjects//CarService//new.db");

            String query2 = "INSERT INTO user (login, password) " +
                    "VALUES ('" + login + "', '" + password + "')";

            Statement statement = connection.createStatement();
            statement.executeUpdate(query2);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void addTime(String day,String hour,String min,int id_count) throws SQLException {
        try {
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:sqlite:C://Users//dragu//IdeaProjects//CarService//new.db");
            String all = day + "-" + hour + "-" + min;
            String query2 = "INSERT INTO Datatime (id, info_time) " +
                    "VALUES ('" + id_count + "', '" + all + "')";

            Statement statement = connection.createStatement();
            statement.executeUpdate(query2);
            datatimes.clear();
            Statement statement2 = connection.createStatement();
            String query = "SELECT id, info_time FROM Datatime";
            ResultSet resultSet2 = statement2.executeQuery(query);
            while (resultSet2.next()) {
                String id = resultSet2.getString("id");
                String info_time = resultSet2.getString("info_time");
                Datatime datatime = new Datatime(id, info_time);
                datatimes.add(datatime);
            }
            resultSet2.close();
            statement2.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void addServ(String title,String price,String info){
        try {
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:sqlite:C://Users//dragu//IdeaProjects//CarService//new.db");

            String query2 = "INSERT INTO Services (info, price,title) " +
                    "VALUES ('" + info + "', '" + price + "', '" + title + "')";

            Statement statement = connection.createStatement();
            statement.executeUpdate(query2);
            statement.close();
            services.clear();
            String query = "SELECT id_serv, info, price, title FROM Services";
            Statement statement2 = connection.createStatement();
            ResultSet resultSet = statement2.executeQuery(query);
            while (resultSet.next()) {
                String id = resultSet.getString("id_serv");
                String info2 = resultSet.getString("info");
                String price2 = resultSet.getString("price");
                String title2 = resultSet.getString("title");
                Service service = new Service(id, info2, price2, title2);
                services.add(service);
            }
            statement2.close();
            resultSet.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "@CarServiceSystemBot";
    }

    @Override
    public String getBotToken() {
        return "6138903115:AAEgovQ-GU0itsUOhOywHGr1jQHihGUCTKg";
    }
}