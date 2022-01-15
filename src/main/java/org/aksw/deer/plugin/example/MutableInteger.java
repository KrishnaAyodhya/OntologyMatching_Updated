package org.aksw.deer.plugin.example;

public class MutableInteger {
    int count = 1;

    public void increment() {
        this.count++;
    }

    public int get(){ return this.count;}
    // getter and setter
}

