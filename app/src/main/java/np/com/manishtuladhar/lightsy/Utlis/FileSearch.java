package np.com.manishtuladhar.lightsy.Utlis;

import java.io.File;
import java.util.ArrayList;

public class FileSearch {

    /**
     * search a directory and return a list of all "directories" contained inside
     * @param directory
     * @return
     */
    public static ArrayList<String> getDirectoryPaths(String directory)
    {
        ArrayList<String> pathArray = new ArrayList<>();
        File file = new File(directory);
        //gives the list of files inside the directory
        //and looping to give all files
        File[] listfiles = file.listFiles();
        for(int i = 0; i<listfiles.length; i++)
        {
            if(listfiles[i].isDirectory())
            {
                pathArray.add(listfiles[i].getAbsolutePath());
            }
        }
        return pathArray;
    }

    /**
     * search a directory and return a list of all "files" contained inside
     * @param directory
     * @return
     */
    public static  ArrayList<String> getFilePath(String directory)
    {
        ArrayList<String> pathArray = new ArrayList<>();
        File file = new File(directory);
        //gives the list of files inside the directory
        //and looping to give all files
        File[] listfiles = file.listFiles();
        for(int i = 0; i<listfiles.length; i++)
        {
            if(listfiles[i].isFile())
            {
                pathArray.add(listfiles[i].getAbsolutePath());
            }
        }
        return pathArray;
    }
}
