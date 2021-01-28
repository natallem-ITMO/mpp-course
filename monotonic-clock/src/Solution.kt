/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Лемешкова Наталья
 */
class Solution : MonotonicClock {
    private var c1 by RegularInt(0)
    private var c2 by RegularInt(0)
    private var c3 by RegularInt(0)
    private var c1_ by RegularInt(0)
    private var c2_ by RegularInt(0)
    private var c3_ by RegularInt(0)


    override fun write(time: Time) {
        // write right-to-left
        c1_ = time.d1
        c2_ = time.d2
        c3_ = time.d3
        c3 = c3_
        c2 = c2_
        c1 = c1_
    }

    override fun read(): Time {
        // read left-to-right
        val r11: Int = c1
        val r12: Int = c2
        val r13: Int = c3

        val r23: Int = c3_
        val r22: Int = c2_
        val r21: Int = c1_
        if (r11 == r21) {
            if (r12 == r22) {
                if (r13 == r23){
                    return Time(r11, r12, r13)
                }
                return Time(r21, r22, r23)
            } else {
                return Time(r21, r22, 0)
            }
        } else {
            return Time(r21, 0, 0)
        }
    }
}