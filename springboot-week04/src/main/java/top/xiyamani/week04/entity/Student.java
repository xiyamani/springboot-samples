package top.xiyamani.week04.entity;

public class Student {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void study(){
        System.out.println(name + "正在学习springboot");
    }
}
