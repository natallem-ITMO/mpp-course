import java.util.concurrent.locks.ReentrantLock;

/**
 * Bank implementation.
 *
 *
 * @author : Лемешкова Наталья
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size


    override fun getAmount(index: Int): Long {
        return accounts[index].amount
    }

    override val totalAmount: Long
        get() {
            accounts.forEach{ account ->
                account.lock.lock()
            }
            val res : Long =  accounts.sumOf { account ->
                account.amount
            }
            accounts.forEach { account -> account.lock.unlock()  }
            return res
        }

    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        try {
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            return account.amount
        } finally {
            account.lock.unlock()
        }
    }

    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        try {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        }finally {
            account.lock.unlock()
        }
    }

    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }

        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        if (fromIndex < toIndex) {
            from.lock.lock()
            try {
                check(amount <= from.amount) { "Underflow" }
                to.lock.lock()
                try {
                    check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                    from.amount -= amount
                    to.amount += amount

                } finally {
                    to.lock.unlock();
                }
            } finally {
                from.lock.unlock();
            }
        } else {
            to.lock.lock()
            try {
                check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                from.lock.lock()
                try {
                    check(amount <= from.amount) { "Underflow" }
                    from.amount -= amount
                    to.amount += amount
                } finally {
                    from.lock.unlock();
                }
            } finally {
                to.lock.unlock();
            }
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        val lock = ReentrantLock();
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0
    }
}