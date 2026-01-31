package org.metabit.platform.support.config;

import java.util.List;

/**
 * multidimensional iterator (here: two-dimensional) for access within a Configuration.
 * see metabit TLVIterator
 *
 * important:
 * the primary use of this cursor is not to explore unknown data structures.
 * it is for accessing those we know, or think we know.
 * 
 * @version $Id: $Id
 */
public interface ConfigCursor // extends Iterable<ConfigEntry>
{
   /**
    * <p>canWrite.</p>
    *
    * @return a boolean
    */
   boolean canWrite();

   // attributes of the current position

   /**
    * <p>isEmpty.</p>
    *
    * @return a boolean
    */
   boolean isEmpty();

   // int getLevel() or getDepth()

   /**
    * <p>isOnList.</p>
    *
    * @return a boolean
    */
   boolean isOnList();

   /**
    * <p>isOnMap.</p>
    *
    * @return a boolean
    */
   boolean isOnMap();

   /**
    * <p>isOnLeaf.</p>
    *
    * @return a boolean
    */
   boolean isOnLeaf();

   /**
    * <p>hasNext.</p>
    *
    * @return a boolean
    */
   boolean hasNext();

   // basic movement

   /**
    * <p>moveNext.</p>
    *
    * @return true if movement was successful, false if not.
    */
   public boolean moveNext();    //

   /**
    * <p>canEnter.</p>
    *
    * @return true if possible, false if not (e.g. leaf).
    */
   public boolean canEnter();    // canMoveDown();

   /**
    * <p>canLeave.</p>
    *
    * @return true if possible, false if not (top level of the structure).
    */
   public boolean canLeave();    // canMoveUp();

   /**
    * enter a constructed tag -> for configurations, enter a branch.
    * Moves into the branch, onto the first element.
    *
    * @return true if movement "into" was successful, and the Iterator is on a valid TLV inside the branch.
    *         if called on a non-constructed tag, it simply returns false.
    *         if called on an empty constructed flag, it returns false as well.
    */
   boolean enter();    // moveDown(); // or "in"

   /**
    * leave a constructed tag we're in.
    * <p>
    * contract: after successful leave(), the Cursor/Iterator is positioned on the
    * constructed TLV it entered.
    * This is because it improves usability and legibility.
    * <p>
    * Sequential/Streaming access will perform a sucessful leave back to the
    * tag that was entered before, but will not allow to re-enter it again.
    *
    * @return true after successfully moving "up" on the branch, onto the constructed TLV.
    *         false if we're at the top level of the structure
    */
   boolean leave();    // moveUp(); // or "out"

   /**
    * <p>moveTo.</p>
    *
    * @param keyWithPath a {@link java.lang.String} object
    * @return a boolean
    */
   boolean moveTo(final String keyWithPath);

   // --- access
   /**
    * <p>getCurrentElement.</p>
    *
    * @return a {@link org.metabit.platform.support.config.ConfigEntry} object
    */
   ConfigEntry getCurrentElement();    // get element as Object


   /**
    * copyMapToObject - copies a map to an object
    *
    * @param targetPojo      java object, to be accessed via reflection setters
    * @param functionPrefix  setter function name prefix, default: "set"
    * @param functionPostfix setter function name postfix, default: ""
    * @return number of entries successfully copied.
    *         <p>
    *         If the use of a setter is not permitted, this is not cause for abort or error.
    *         It may be logged, but otherwise, operation continues.
    *         <p>
    *         It is an error, however, if this function is called while the cursor is
    *         not on a map ( isOnMap() == false ).
    */
   int copyMapToObject(Object targetPojo, final String functionPrefix, final String functionPostfix);
   /// -----


   // --- if writing is possible, only ---
   /**
    * OLD DOCUMENTATION from TLVIterator
    * derived function:
    * Removes from the underlying collection the last element returned
    * by this iterator (optional operation).  This method can be called
    * only once per call to next().  The behavior of an iterator
    * is unspecified if the underlying collection is modified while the
    * iteration is in progress in any way other than by calling this
    * method.
    *
    * @throws java.lang.UnsupportedOperationException if the {@code remove}
    *                                       operation is not supported by this iterator
    * @throws java.lang.IllegalStateException         if the {@code next} method has not
    *                                       yet been called, or the {@code remove} method has already
    *                                       been called after the last call to the {@code next}
    *                                       method
    */
   void remove();

   // --------------------------------------------------------------------------
    // writing via ConfigCursor not supported in current version.

   /**
    * Writes a value to the configuration at the current cursor position.
    *
    * @param key   the key of the entry
    * @param value the value to be written
    * @param scope the scope the entry is to be written to
    * @throws ConfigException on failure
    */
   default void put(String key, Object value, ConfigScope scope) throws ConfigException
   {
       throw new UnsupportedOperationException("put is not implemented for this cursor");
   }


}
//___EOF___

