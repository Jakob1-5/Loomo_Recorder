package com.example.loomorecorder


class RingBuffer<T>(bufferSize: Int = 10, val allowOverwrite: Boolean = false) {
    private val bufferContents = mutableListOf<T?>().apply {
        for (index in 0 until bufferSize) {
            this.add(null)
        }
    }
//    private val bufferContents = mutableListOf<T>()

    var bufferSize = bufferSize
        private set
    var head = 0        // Head: 'oldest' entry (read index)
        private set
    var tail = 0        // Tail: 'newest' entry (write index)
        private set
    var itemsInQueue = 0    // N.o. items in the queue
        private set

    /**
     * Clears the references to the contents of the buffer. The buffer
     * still occupies the same amount of memory
     */
    fun clearContents() {
        head = 0
        tail = 0
        itemsInQueue = 0
    }

    fun clear() {
        bufferContents.clear()
        clearContents()
    }
    fun clearAndResetBufferSize(newSize:Int = 10) {
        bufferContents.clear()
        bufferSize = newSize
        clearContents()
        bufferContents.apply {
            for (index in 0 until bufferSize) {
                this.add(null)
            }
        }
    }

    fun increaseBufferSizeBy(n: Int) {
        bufferSize += n
        bufferContents.apply {
            for (index in (tail+1) until (tail+1+n)) {
                this.add(index, null)
            }
        }
        if(head > tail) head += n
    }

    fun enqueue(item: T): RingBuffer<T> {
        // The functions in this class can be called asynchronously,
        // so a temporary val is used for the tail index so that e.g. peek()
        // doesn't access the tail before something has been put there
        //TODO: do similar stuff to rest of class to make it more thread-safe
        val tmpTail = if (itemsInQueue != 0) {
            (tail + 1) % bufferSize
        } else tail
        if (itemsInQueue == bufferSize) {
            if (allowOverwrite) {
                head = (head + 1) % bufferSize
            } else {
                throw OverflowException("Queue is full, can't add $item")
            }
        } else {
            ++itemsInQueue
        }

        bufferContents[tmpTail] = item
        tail = tmpTail

        return this
    }
    fun enqueue() {
        val tmpTail = if (itemsInQueue != 0) {
            (tail + 1) % bufferSize
        } else tail
        if (itemsInQueue == bufferSize) {
            if (allowOverwrite) {
                head = (head + 1) % bufferSize
            } else {
                throw OverflowException("Queue is full, can't enqueue")
            }
        } else {
            ++itemsInQueue
        }
        tail = tmpTail
    }

    fun dequeue(): T? {
        if (itemsInQueue == 0) {
            throw EmptyBufferException("Queue is empty, can't dequeue()")
        }

//        val item = bufferContents[head % (bufferContents.size)]
        val item = bufferContents[head]
        itemsInQueue--
        if (itemsInQueue != 0) {
            head = (head + 1) % bufferSize
        }

        return item
    }

    fun peek(tailOffset: Int = 0): T? {
        var offset = tailOffset
        if (offset > itemsInQueue) {
            offset = itemsInQueue
        }
        val index = if (offset <= tail) {
            tail - offset
        } else {
            bufferSize - (offset - tail)
        }
        return if (bufferContents.isNotEmpty()) bufferContents[index] //?: throw EmptyBufferException("Buffer is empty")
        else throw EmptyBufferException("RingBuffer is empty")
    }

    fun peekHead(): T? = bufferContents[head]

    fun getContents(): MutableList<T?> {
        return mutableListOf<T?>().apply {
            var itemCount = itemsInQueue
            var readIndex = head
            while (itemCount > 0) {
                add(bufferContents[readIndex])
                readIndex = (readIndex + 1) % bufferSize
                itemCount--
            }
        }
    }

    fun isEmpty() = itemsInQueue == 0
    fun isNotEmpty() = itemsInQueue != 0
    fun freeSpace() = bufferSize - itemsInQueue
    operator fun get(index: Int): T? {
        return bufferContents[index]
    }

    override fun toString(): String {
//        var str = "Size: $bufferSize\nItems in queue: $itemsInQueue"
//        if (bufferSize > 20) {
//            str += "\nHead (index: $head): ${peekHead()}\nTail (index: $tail): ${peek()}"
//        } else {
//            for (a in bufferContents) {
//                str += "\n"
//                str += a.toString()
//                if (a == peekHead()) str += ", head"
//                if (a == peek()) str += ", tail"
//            }
//        }
//        return str
        return "Nom size: $bufferSize, real size: ${bufferContents.size}, head: $head, tail: $tail, itemsInQueue: $itemsInQueue"
    }
}

class OverflowException(msg: String) : RuntimeException(msg)
class EmptyBufferException(msg: String) : RuntimeException(msg)



//// Obs: this class does no pre-allocation of its container (MutableList<T>), which might have a performance impact
//class RingBuffer<T>(bufferSize: Int = 10, val allowOverwrite: Boolean = false) {
//    //    private val bufferContents = mutableListOf<T?>().apply {
////        for (index in 0 until maxSize) {
////            add(null)
////        }
////    }
//    private val bufferContents = mutableListOf<T>()
//
//    var bufferSize = bufferSize
//        private set
//    var head = 0        // Head: 'oldest' entry (read index)
//        private set
//    var tail = 0        // Tail: 'newest' entry (write index)
//        private set
//    var itemsInQueue = 0    // N.o. items in the queue
//        private set
//
//    /**
//     * Clears the references to the contents of the buffer. The buffer
//     * still occupies the same amount of memory
//     */
//    fun clearContents() {
//        head = 0
//        tail = 0
//        itemsInQueue = 0
//    }
//
//    fun clear() {
//        bufferContents.clear()
//        clearContents()
//    }
//    fun clearAndResetBufferSize(newSize:Int = 10) {
//        bufferContents.clear()
//        bufferSize = newSize
//        clearContents()
//    }
//
//    fun increaseBufferSizeBy(n: Int) {
//        bufferSize += n
//    }
//
//    fun enqueue(item: T): RingBuffer<T> {
//        // The functions in this class can be called asynchronously,
//        // so a temporary val is used for the tail index so that e.g. peek()
//        // doesn't access the tail before something has been put there
//        //TODO: do similar stuff to rest of class to make it more thread-safe
//        val tmpTail = if (itemsInQueue != 0) {
//            (tail + 1) % bufferSize
//        } else tail
//        if (itemsInQueue == bufferSize) {
//            if (allowOverwrite) {
//                head = (head + 1) % bufferSize
//            } else {
//                throw OverflowException("Queue is full, can't add $item")
//            }
//        } else {
//            ++itemsInQueue
//        }
//
//        if (bufferContents.size <= bufferSize) {
//            bufferContents.add(tmpTail, item)
//            head %= bufferContents.size //
//        }
//        else bufferContents[tmpTail] = item
//        tail = tmpTail
//
//        return this
//    }
//
//    fun dequeue(): T {
//        if (itemsInQueue == 0) {
//            throw EmptyBufferException("Queue is empty, can't dequeue()")
//        }
//
//        val item = bufferContents[head % (bufferContents.size)]
////        val item = bufferContents[head]
//        head = (head + 1) % bufferSize
//        itemsInQueue--
//
//        return item
//    }
//
//    fun peek(tailOffset: Int = 0): T {
//        var offset = tailOffset
//        if (offset > itemsInQueue) {
//            offset = itemsInQueue
//        }
//        val index = if (offset <= tail) {
//            tail - offset
//        } else {
//            bufferSize - (offset - tail)
//        }
//        return if (bufferContents.isNotEmpty()) bufferContents[index] //?: throw EmptyBufferException("Buffer is empty")
//        else throw EmptyBufferException("RingBuffer is empty")
//    }
//
//    fun peekHead(): T = bufferContents[head]
//
//    fun getContents(): MutableList<T> {
//        return mutableListOf<T>().apply {
//            var itemCount = itemsInQueue
//            var readIndex = head
//            while (itemCount > 0) {
//                add(bufferContents[readIndex])
//                readIndex = (readIndex + 1) % bufferSize
//                itemCount--
//            }
//        }
//    }
//
//    fun isEmpty() = itemsInQueue == 0
//    fun isNotEmpty() = itemsInQueue != 0
//    fun freeSpace() = bufferSize - itemsInQueue
//    operator fun get(index: Int): T? {
//        return bufferContents[index]
//    }
//
//}
//
//class OverflowException(msg: String) : RuntimeException(msg)
//class EmptyBufferException(msg: String) : RuntimeException(msg)