package com.gee12.mytetroid.data;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static List<TetroidNode> rootNodesCollection;
    private static List<TetroidRecord> recordsCollection;

    static String html = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">\n" +
            "<html><head><meta name=\"qrichtext\" content=\"1\" /><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><style type=\"text/css\">p, li { white-space: pre-wrap; }</style>\n" +
            "</head><body style=\" font-family:'DejaVu Sans'; font-size:11pt; font-weight:400; font-style:normal;\">\n" +
            "<p style=\" margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\">2016г</p>\n" +
            "<p style=\" margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\">Это функция в Android, которая позволяет пользоваться приложениями без их скачивания и установки. Отдельные модули приложения, необходимые пользователю, просто подгружаются из Google Play.</p>\n" +
            "<p style=\"-qt-paragraph-type:empty; margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\"><br /></p>\n" +
            "<p style=\" margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\">Пример:</p>\n" +
            "<p style=\" margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\">Ты при помощи телефона или планшета с Android ищешь в интернете какой-то товар и жмешь на ссылку в поисковой выдаче. И вместо сайта магазина твое устройство загрузит некую минимальную версию приложения (размером ~1Мб), в которой будет только информация об этом товаре и кнопка Купить.</p>\n" +
            "<p style=\"-qt-paragraph-type:empty; margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\"><br /></p>\n" +
            "<p style=\" margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\">Во-первых, ты можешь немедленно совершить покупку, используя уже имеющиеся в телефоне данные кредитной карты. Во-вторых, с твоего разрешения приложение может получить доступ к датчикам телефона и сохраненной в нем информации. В-третьих, у приложения может быть более отзывчивый интерфейс, чем у сайта.</p>\n" +
            "<p style=\"-qt-paragraph-type:empty; margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\"><br /></p>\n" +
            "<p style=\" margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\">Технически это реализовано следующим образом: разработчик строит свою программу так, чтобы у нее была легко отделимая часть, которую можно загружать отдельно. Создавать новую ветку кода при этом не обязательно, главное — в нужном месте вызвать программные интерфейсы Instant Apps. Приложение отправляется в Google Play, и остальное — это уже магия Google. Когда поисковик решит, что вместо сайта можно показать приложение, он запросит его из Google Play и покажет пользователю.</p>\n" +
            "<p style=\"-qt-paragraph-type:empty; margin-top:0px; margin-bottom:0px; margin-left:0px; margin-right:0px; -qt-block-indent:0; text-indent:0px;\"><br /></p></body></html>";

    public static boolean init(String dataFolderPath) {
        try {
            FileInputStream fis = new FileInputStream(dataFolderPath + "/mytetra.xml");
            rootNodesCollection = new XMLManager().parse(fis);
        } catch (Exception ex) {
            rootNodesCollection = initFake();
            return false;
        }
        return true;
    }

    public static List<TetroidNode> initFake() {
        TetroidRecord firstRec = new TetroidRecord("1", "First record", html);
        TetroidRecord secondRec = new TetroidRecord("2", "Second record");
        TetroidRecord thirdRec = new TetroidRecord("3", "Third record");
        TetroidRecord fourthRec = new TetroidRecord("4", "Fourth record");
        TetroidRecord fifthRec = new TetroidRecord("5", "Sixth record");
        TetroidRecord sixthRec = new TetroidRecord("6", "Sixth record");
        TetroidRecord seventhRec = new TetroidRecord("7", "Seventh record");
        TetroidRecord eighthRec = new TetroidRecord("8", "Eighth record");
        TetroidRecord ninethRec = new TetroidRecord("9", "Ninth record");

        TetroidNode first = new TetroidNode("1","First",0);
        first.addRecord(firstRec);
        first.addRecord(secondRec);
        first.addRecord(thirdRec);
        first.addRecord(fourthRec);
        first.addRecord(fifthRec);
        TetroidNode second = new TetroidNode("2","Second",1);
        second.addRecord(sixthRec);
        second.addRecord(seventhRec);
        TetroidNode third = new TetroidNode("3","Third",1);
        TetroidNode fourth = new TetroidNode("4","Fourth",1);
        TetroidNode fifth  = new TetroidNode("5","Fifth",2);
        TetroidNode sixth = new TetroidNode("6","Sixth",2);
        sixth.addRecord(eighthRec);
        TetroidNode seventh = new TetroidNode("7","Seventh",3);
        TetroidNode eighth = new TetroidNode("8","Eighth",0);
        TetroidNode nineth = new TetroidNode("9","Ninth",0);
        nineth.addRecord(ninethRec);

        List<TetroidNode> nodes = new ArrayList<>();
        first.addSubNode(second);
        first.addSubNode(third);
            third.addSubNode(fifth);
            third.addSubNode(sixth);
                sixth.addSubNode(seventh);
        nodes.add(first);
        eighth.addSubNode(fourth);
        nodes.add(eighth);
        nodes.add(nineth);
        return nodes;
    }

    public static List<TetroidNode> getRootNodes() {
        return rootNodesCollection;
    }
}
