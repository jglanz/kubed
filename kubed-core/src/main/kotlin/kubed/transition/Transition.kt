package kubed.transition

import javafx.animation.*
import javafx.scene.CacheHint
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Shape
import javafx.util.Duration
import kubed.ease.CubicInOutInterpolator
import kubed.selection.AbstractSelection
import kubed.selection.Selection
import kubed.selection.removeNode

class Transition<T> internal constructor(val parent: Transition<T>?, internal var selection: Selection<T>, val name: String = "")
    : AbstractSelection<Transition<T>, T>() {
    companion object {
        const val COALESCE_MS = 17.0
    }

    internal val metadata: MutableMap<Node, TransitionMetadata> = HashMap()

    private var remove: ((Node) -> Unit)? = null

    private var index = 0

    private val transitions = HashMap<Node, ParallelTransition>()

    init {
        if(parent == null) {
            val coalescer = PauseTransition(Duration.millis(COALESCE_MS))
            coalescer.statusProperty().addListener { _, _, status ->
                if(status == Animation.Status.STOPPED) {
                    prepareTransitions()
                    playTransitions()
                    //getTransition(this, index)?.play()
                }
            }
            coalescer.play()
        }
        else {
            index = parent.index + 1
            parent.on(Animation.Status.STOPPED) {
                prepareTransition(this)
                getTransition(this, index)?.play()
            }
        }
    }

    fun transition(): Transition<T> {
        val t = Transition(this, Selection(selection), name)
        metadata.forEach { k, (node, _, duration, interpolator, cacheHint) ->
            t.metadata[k] = TransitionMetadata(node, Duration.ZERO, duration, interpolator, cacheHint)
        }
        return t
    }

    fun delay(delay: (d: T, i: Int, nodes: List<Node?>) -> Duration): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            var ms = delay(d, i, group).toMillis()
            if(parent == null)
                ms -= - COALESCE_MS

            getMetadata(this).delay = if(ms <= 0) Duration.ZERO else Duration.millis(ms)
        }

        return this
    }

    fun delay(delay: Duration): Transition<T> { 
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    var ms = delay.toMillis()
                    if(parent == null)
                        ms -= COALESCE_MS

                    getMetadata(it).delay = if(ms <= 0) Duration.ZERO else Duration.millis(ms)
                }

        return this
    }

    fun duration(delay: (d: T, i: Int, nodes: List<Node?>) -> Duration): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getMetadata(this).duration = delay(d, i, group)
        }

        return this
    }

    fun duration(duration: Duration): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach { getMetadata(it).duration = Duration.millis(duration.toMillis()) }

        return this
    }

    fun cycleCount(cycleCount: (d: T, i: Int, nodes: List<Node?>) -> Int): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.cycleCount = cycleCount(d, i, group)
        }

        return this
    }

    fun cycleCount(cycleCount: Int): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach { getTransition(it, index)?.cycleCount = cycleCount }

        return this
    }

    fun autoReverse(autoReverse: (d: T, i: Int, nodes: List<Node?>) -> Boolean): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.isAutoReverse = autoReverse(d, i, group)
        }

        return this
    }

    fun autoReverse(autoReverse: Boolean): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach { getTransition(it, index)?.isAutoReverse = autoReverse }

        return this
    }

    override fun opacity(value: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(fadeTo(value(d, i, group), play = false))
        }

        return this
    }

    override fun opacity(value: Double): Transition<T> {
        selection.groups.flatMap { it }
                 .filterIsInstance<Node>()
                 .forEach {
                     getTransition(it, index)?.children?.add(it.fadeTo(value, play = false))
                 }

        return this
    }

    fun rotate(angle: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(rotateTo(angle(d, i, group), play = false))
        }

        return this
    }

    fun rotate(angle: Double): Transition<T> {
        selection.groups.flatMap { it }
                        .filterIsInstance<Node>()
                        .forEach { getTransition(it, index)?.children?.add(it.rotateTo(angle, play = false)) }

        return this
    }

    override fun rotateX(angle: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(rotateXTo(angle(d, i, group), play = false))
        }

        return this
    }

    override fun rotateX(angle: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.rotateXTo(angle, play = false))
                }

        return this
    }

    override fun rotateY(angle: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(rotateYTo(angle(d, i, group), play = false))
        }

        return this
    }

    override fun rotateY(angle: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.rotateYTo(angle, play = false))
                }

        return this
    }

    override fun rotateZ(angle: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(rotateZTo(angle(d, i, group), play = false))
        }

        return this
    }

    override fun rotateZ(angle: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.rotateZTo(angle, play = false))
                }

        return this
    }

    override fun scaleX(x: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(scaleXTo(x(d, i, group), play = false))
        }

        return this
    }

    override fun scaleX(x: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.scaleXTo(x, play = false))
                }

        return this
    }

    override fun scaleY(y: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(scaleYTo(y(d, i, group), play = false))
        }

        return this
    }

    override fun scaleY(y: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.scaleYTo(y, play = false))
                }

        return this
    }

    override fun scaleZ(z: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(scaleZTo(z(d, i, group), play = false))
        }

        return this
    }

    override fun scaleZ(z: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.scaleZTo(z, play = false))
                }

        return this
    }

    override fun translateX(x: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(translateXTo(x(d, i, group), play = false))
        }

        return this
    }

    override fun translateX(x: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                        getTransition(it, index)?.children?.add(it.translateXTo(x, play = false))
                }

        return this
    }

    override fun translateY(y: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(translateYTo(y(d, i, group), play = false))
        }

        return this
    }

    override fun translateY(y: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.translateYTo(y, play = false))
                }

        return this
    }

    override fun translateZ(z: (d: T, i: Int, group: List<Node?>) -> Double): Transition<T> {
        selection.forEach<Node> { d, i, group ->
            getTransition(this, index)?.children?.add(translateZTo(z(d, i, group), play = false))
        }
        return this
    }

    override fun translateZ(z: Double): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    getTransition(it, index)?.children?.add(it.translateZTo(z, play = false))
                }

        return this
    }

    fun interpolator(interpolator: (d: T, i: Int, group: List<Node?>) -> Interpolator): Transition<T> {
        selection.forEach<Node>({ d, i, group -> getMetadata(this).interpolator = interpolator(d, i, group) })
        return this
    }

    fun interpolator(interpolator: Interpolator): Transition<T> { 
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach { getMetadata(it).interpolator = interpolator }

        return this
    }

    fun ease(interpolator: Interpolator) = interpolator(interpolator)

    fun ease(interpolator: (d: T, i: Int, group: List<Node?>) -> Interpolator) = interpolator(interpolator)

    fun cacheHint(hint: (d: T, i: Int, group: List<Node?>) -> CacheHint): Transition<T> {
        selection.forEach<Node> { d, i, group -> getMetadata(this).cacheHint = hint(d, i, group) }
        return this
    }

    fun cacheHint(hint: CacheHint): Transition<T> { 
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach { getMetadata(it).cacheHint = hint }

        return this
    }

    override fun fill(paint: (d: T, i: Int, group: List<Node?>) -> Paint?): Transition<T> {
        selection.forEach<Shape> { d, i, group ->
            getTransition(this, index)?.children?.add(fillTo(paint(d, i, group) as Color))
        }

        return this
    }

    override fun fill(paint: Paint?): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Shape>()
                .forEach { getTransition(it, index)?.children?.add(it.fillTo((paint as Color?) ?: Color.TRANSPARENT)) }

        return this
    }

    override fun stroke(paint: Paint?): Transition<T> {
        selection.groups.flatMap { it }
                .filterIsInstance<Shape>()
                .forEach { getTransition(it, index)?.children?.add(it.strokeTo((paint as Color?) ?: Color.TRANSPARENT)) }

        return this
    }

    override fun stroke(paint: (d: T, i: Int, group: List<Node?>) -> Paint?): Transition<T> {
        selection.forEach<Shape> { d, i, group ->
            getTransition(this, index)?.children?.add(strokeTo((paint(d, i, group) as Color?) ?: Color.TRANSPARENT))
        }

        return this
    }

    fun on(status: Animation.Status, handler: Node.() -> Unit) {
        selection.groups.flatMap { it }
                .filterIsInstance<Node>()
                .forEach {
                    val t = getTransition(it, index)
                    if(t != null) {
                        //if(t.status == status)
                        //    it.handler()
                       // else {
                            t.statusProperty()?.addListener { _, _, newStatus ->
                                if(newStatus == status)
                                    it.handler()
                            }
                       // }
                    }
                }
    }

    fun remove(remove: (node: Node) -> Unit = ::removeNode): Transition<T> { 
        this.remove = remove

        // Consider: Should we remove at the end of the nodes transition, or at the end of the transition as a whole?
        on(Animation.Status.STOPPED) {
            remove(this)
        }

        return this
    }

    private fun getMetadata(node: Node): TransitionMetadata {
        var m = metadata[node]
        if(m == null) {
            m = TransitionMetadata(node)
            metadata[node] = m
        }

        return m
    }

    private fun getTransition(node: Node, i: Int): ParallelTransition? {
        var transitions = node.getTransitions()
        if(transitions == null) {
            transitions = HashMap()
            node.setTransitions(transitions)
        }

        var map = transitions[name]
        if(map == null) {
            map = HashMap()
            transitions[name] = map
        }

        if(map.size - 1 < i)
            map[i] = this

        if(!map[i]!!.transitions.contains(node))
            map[i]!!.transitions[node] = ParallelTransition()

        return map[i]?.transitions?.get(node)
    }

    private fun prepareTransitions() = metadata.keys.forEach { prepareTransition(it) }

    private fun prepareTransition(node: Node) {
        val m = metadata[node]
        val t = getTransition(node, index)
        if(t != null) {
            if(t.children.isEmpty())
                t.children += PauseTransition(m?.delay ?: Duration.ZERO)
            else {
                t.delay = m?.delay ?: Duration.ZERO
                t.interpolator = m?.interpolator ?: CubicInOutInterpolator()
                t.children.forEach { setDuration(it, m?.duration ?: DEFAULT_DURATION) }
            }

            val originalCacheHint = node.cacheHint
            t.statusProperty().addListener { _, _, status ->
                when(status) {
                    Animation.Status.RUNNING -> node.cacheHint = m?.cacheHint ?: node.cacheHint
                    Animation.Status.PAUSED,
                    Animation.Status.STOPPED, null -> node.cacheHint = originalCacheHint
                }
            }
        }
        else {

        }
    }

    private fun playTransitions() = metadata.keys.forEach {
        getTransition(it, index)?.play()
    }

    private fun setDuration(a: Animation, duration: Duration) {
        when(a) {
            is FadeTransition -> a.duration = duration
            is FillTransition -> a.duration = duration
            is PathTransition -> a.duration = duration
            is PauseTransition -> a.duration = duration
            is RotateTransition -> a.duration = duration
            is ScaleTransition -> a.duration = duration
            is StrokeTransition -> a.duration = duration
            is TranslateTransition -> a.duration = duration
        }
    }
}

internal data class TransitionMetadata(val node: Node,
                                       var delay: Duration = Duration.ZERO,
                                       var duration: Duration = DEFAULT_DURATION,
                                       var cacheHint: CacheHint = node.cacheHint,
                                       var interpolator: Interpolator = CubicInOutInterpolator())