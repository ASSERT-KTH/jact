package jact.test.project;

public class Foo {
    public boolean bar(int x) {
        helloWorld();
        if(x > 10){
            return true;
        }else{
            return false;
        }
    }
    public void helloWorld(){
        System.out.println("Hello, World!");
    }
}
