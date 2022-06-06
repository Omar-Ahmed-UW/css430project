//Phillip Burlachenko Spring 22 file system prog5ez Task4
import java.io.FilenameFilter;
import java.util.*; // SysLib_org.java

public class SysLib {
/*************************************ADDED METHODS FROM PROG5EZ***************************************/
    /**
     * Format method used to format the blocks in the file system
     * @param files number of blocks (files) to format on the disk 
     * @return kernel.Format 
     */
    public static int format(int files){
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
                Kernel.FORMAT, files, null);
    }
    
    /**
     * Open method opens a file that will be used in the filesystem 
     * and has the abilit to open in either read/write modes (passed in)
     * @param fileName name of file that is to be opened
     * @param mode read/write mode 
     * @return kernel.OPEN
     */
    public static int open(String fileName, String mode){
        String[] args = new String[2];
        args[0] = fileName;
        args[1] = mode;
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
                Kernel.OPEN, 0, args);
    }

    /**
     * Read method reads the bytes from a passed in file descripter to a byte array buffer
     * @param fd file descripter (used to read from)
     * @param buffer byte array to read from 
     * @return kernel.READ
     */
    public static int read(int fd, byte buffer[]){
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
                Kernel.READ, fd, buffer);
    }

    /**
     * Write method writes the bytes from a passed in file descripter to a byte array buffer
     * @param fd file descripter (used to write to)
     * @param buffer byte array to write to
     * @return kernel.write
     */
    public static int write(int fd, byte buffer[]){
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
                Kernel.WRITE, fd, buffer);
    }

    /**
     * Seek method updates the seek pointer correspond to fd. The pointer can move forwards or in reverse. 
     * @param fd file descripter for the file using the seek pointer
     * @param offset the offset value for the file pointer
     * @param whence where the seek method operates from 
     * @return
     */
    public static int seek(int fd, int offset, int whence){
        int[] args = new int[2];
        args[0] = offset;
        args[1] = whence;
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
                Kernel.SEEK, fd, args);
    }

    /**
     * Close method used to close the file corresponding to the file descriptor. 
     * @param fd file descripter
     * @return Kernel.CLOSE
     */
    public static int close(int fd){
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
                Kernel.CLOSE, fd, null);
    }

    /**
     * Delete method helps delete a file specified by file name. 
     * @param fileName string of file name
     * @return kernel.delete
     */
    public static int delete(String fileName){
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
            Kernel.DELETE, 0, fileName);
    }

    /**
     * fsize method returns the size in bytes of the file indicated by the file descripter 
     * @param fd file descriptor 
     * @return kernel.size 
     */
    public static int fsize(int fd){
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, 
            Kernel.SIZE, fd, null);
    }
   
/*************************************END OF ADDED METHODS FROM PROG5EZ***************************************/    

    public static int exec( String args[] ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXEC, 0, args );
    }

    public static int join( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WAIT, 0, null );
    }

    public static int boot( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.BOOT, 0, null );
    }

    public static int exit( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXIT, 0, null );
    }

    public static int sleep( int milliseconds ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SLEEP, milliseconds, null );
    }

    public static int disk( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_DISK,
				 0, 0, null );
    }

    public static int cin( StringBuffer s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.READ, 0, s );
    }

    public static int cout( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 1, s );
    }

    public static int cerr( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 2, s );
    }

    public static int rawread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWREAD, blkNumber, b );
    }

    public static int rawwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWWRITE, blkNumber, b );
    }

    public static int sync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SYNC, 0, null );
    }

    public static int cread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CREAD, blkNumber, b );
    }

    public static int cwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CWRITE, blkNumber, b );
    }

    public static int flush( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CFLUSH, 0, null );
    }

    public static int csync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CSYNC, 0, null );
    }

    public static String[] stringToArgs( String s ) {
	StringTokenizer token = new StringTokenizer( s," " );
	String[] progArgs = new String[ token.countTokens( ) ];
	for ( int i = 0; token.hasMoreTokens( ); i++ ) {
	    progArgs[i] = token.nextToken( );
	}
	return progArgs;
    }

    public static void short2bytes( short s, byte[] b, int offset ) {
	b[offset] = (byte)( s >> 8 );
	b[offset + 1] = (byte)s;
    }

    public static short bytes2short( byte[] b, int offset ) {
	short s = 0;
        s += b[offset] & 0xff;
	s <<= 8;
        s += b[offset + 1] & 0xff;
	return s;
    }

    public static void int2bytes( int i, byte[] b, int offset ) {
	b[offset] = (byte)( i >> 24 );
	b[offset + 1] = (byte)( i >> 16 );
	b[offset + 2] = (byte)( i >> 8 );
	b[offset + 3] = (byte)i;
    }

    public static int bytes2int( byte[] b, int offset ) {
	int n = ((b[offset] & 0xff) << 24) + ((b[offset+1] & 0xff) << 16) +
	        ((b[offset+2] & 0xff) << 8) + (b[offset+3] & 0xff);
	return n;
    }

}
