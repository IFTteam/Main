package springredis.demo.structures;

import springredis.demo.entity.Event;

import java.util.Date;
import java.util.LinkedList;


public class MinHeap {
    private static LinkedList<Event> arr = new LinkedList<Event>();;

    public static LinkedList<Event> getArr() {
        return arr;
    }

    public static void setArr(LinkedList<Event> arr) {
        MinHeap.arr = arr;
    }
    public static Date getTopTime(){
        return arr.get(0).getTriggerTime();
    }

    public static Boolean isEmpty(){
        return arr.isEmpty();
    }



    public static Event heapPop(){
        swap(0, arr.size()-1);
        Event event = arr.pollLast();
        heapify(0);
        return event;
    }

    public static void heapInsert(Event event) {
        arr.add(event);
        int index = arr.size()-1;
        while (arr.get(index).getTriggerTime().getTime() < arr.get((index - 1) / 2).getTriggerTime().getTime()) {
            swap(index, (index - 1) /2);
            index = (index - 1)/2 ;
        }
    }

    public static void heapify(int index) {
        int size = arr.size();
        int left = index * 2 + 1;
        while (left < size) {
            int smallest = left + 1 < size && arr.get(left + 1).getTriggerTime().getTime() < arr.get(left).getTriggerTime().getTime() ? left + 1 : left;
            smallest = arr.get(smallest).getTriggerTime().getTime() < arr.get(index).getTriggerTime().getTime() ? smallest : index;
            if (smallest == index) {
                break;
            }
            swap(smallest, index);
            index = smallest;
            left = index * 2 + 1;
        }
    }

    public static void swap(int i, int j) {
        Event tmp = arr.get(i);
        arr.set(i,arr.get(j));
        arr.set(j,tmp);
    }
}