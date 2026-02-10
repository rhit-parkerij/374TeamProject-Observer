package domain;

public class MethodCall {
    String owner;
    String name;
    String desc;

    public MethodCall(String owner, String name){
        this.owner = owner;
        this.name = name;
    }

    public MethodCall(String owner, String name, String desc){
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
