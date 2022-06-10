/*
 * 
 * Author: Omar Ahmed
 * File: Directory.java
 * Date: 6/6/22
 * 
 */
public class Directory {
    private static int maxChars = 30;
    private int[] fsizes;
    private char[][] fnames;

    public Directory( int maxInumber ) { // directory constructor 
        fsizes = new int[maxInumber];     // maxInumber = max files 
        for ( int i = 0; i < maxInumber; i++ )  
            fsizes[i] = 0;                 // all file size initialized to 0 
        fnames = new char[maxInumber][maxChars]; 
        String root = "/";                // entry(inode) 0 is "/" 
        fsizes[0] = root.length( );        // fsize[0] is the size of "/". 
        root.getChars( 0, fsizes[0], fnames[0], 0 ); // fnames[0] includes "/" 
    } 

    public void bytes2directory( byte data[] ) { 
        // assumes data[] contains directory information retrieved from disk                                                      
        // initialize the directory fsizes[] and fnames[] with this data[] 
        if(data.length == 0|| data == null) {return;}                                                      
        int offset = 0; 
        for ( int i = 0; i < fsizes.length; i++, offset += 4 ) 
            fsizes[i] = SysLib.bytes2int( data, offset ); 
 
        for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) { 
            String fname = new String( data, offset, maxChars * 2 ); 
            fname.getChars( 0, fsizes[i], fnames[i], 0 ); 
        } 
      } 

    public byte[] directory2bytes( ) { 
        // converts and return directory information into a plain byte array                                                      
        // this byte array will be written back to disk                                                                           
        byte[] data = new byte[fsizes.length * 4 + fnames.length * maxChars * 2]; 
        int offset = 0; 
        for ( int i = 0; i < fsizes.length; i++, offset += 4 ) 
            SysLib.int2bytes( fsizes[i], data, offset ); 
 
        for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) { 
            String tableEntry = new String( fnames[i], 0, fsizes[i] ); 
            byte[] bytes = tableEntry.getBytes( ); 
            System.arraycopy( bytes, 0, data, offset, bytes.length ); 
        } 
        return data; 
    } 

    public short ialloc(String filename) {
        if(filename == null || filename.isEmpty()){return -1;}
        if(filename.length() > maxChars){return Kernel.ERROR;}
        for (int s = 1; s < fsizes.length; s++) {
            if (fsizes[s] == 0){
                fsizes[s] = Math.min(filename.length(), maxChars);
                filename.getChars(0, fsizes[s], fnames[s], 0);
                return (short)s;
            }
        }
        return -1;
    }

    public boolean ifree(short iNumber) {
        if (fsizes[iNumber] > 0) {
            fsizes[iNumber] = 0;
            return true;
        }
        return false;
    }

    public short namei(String filename) {
        if(filename == null || filename.isEmpty()){return -1;}
        for (int s = 0; s < fsizes.length; s = s + 1) {
            String otherFileName;
            if (fsizes[s] == filename.length() || filename.compareTo(otherFileName = new String(fnames[s], 0, fsizes[s])) == 0){
                return (short)s;
            }
        }
        return -1;
    }
}