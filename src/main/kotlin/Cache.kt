class Cache<T : Any>(private val block: () -> T) {
	private var cacheValue: T? = null
	
	fun clear() {
		set(null)
	}
	
	fun set(value: T?) {
		cacheValue = value
	}
	
	fun get(): T {
		return if (cacheValue == null) {
			block.invoke()
		} else {
			cacheValue!!
		}
	}
	
	fun getAndSet(): T {
		val value = get()
		set(value)
		return value
	}
}