public class Service {
    private String id;
    private String info;
    private String price;
    private String title;
    public Service(String id, String info, String price, String title) {
        this.id = id;
        this.info = info;
        this.price = price;
        this.title = title;
    }
    public String getId() {
        return id;
    }
    public String getInfo() {
        return info;
    }
    public String getPrice() {
        return price;
    }
    public String getTitle() {
        return title;
    }
}