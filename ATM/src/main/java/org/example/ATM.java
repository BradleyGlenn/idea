package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class ATM {

    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/zhenxue";
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "123456";

    private ArrayList<Account> accounts = new ArrayList<>();

    private Scanner sc = new Scanner(System.in);
    private Account loginAcc; // 记住登录后的用户账户

    // 数据库连接相关信息
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/zhenxue";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";

    // 创建数据库连接
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    // 插入用户信息到数据库
    private void insertUserToDatabase(Account acc) {
        Connection connection = null;  // 声明在 try 之前

        try {
            connection = createConnection();
            connection.setAutoCommit(false);

            if (!isCardIdUnique(acc.getCardId(), connection)) {
                System.out.println("卡号已存在，请检查输入");
                return;
            }

            String sql = "INSERT INTO ATM_user (Username, cardId, sex, passWord, money, `limit`) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, acc.getUsername());
                preparedStatement.setString(2, acc.getCardId());
                preparedStatement.setString(3, String.valueOf(acc.getSex()));
                preparedStatement.setString(4, acc.getPassWord());
                preparedStatement.setDouble(5, acc.getMoney());
                preparedStatement.setDouble(6, acc.getLimit());

                preparedStatement.executeUpdate();

                int generatedCardId = -1; // 声明在try之外，以确保在整个方法内可见

                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedCardId = generatedKeys.getInt(1);
                        System.out.println("DEBUG: 生成的卡号为：" + generatedCardId);

                        acc.setCardId(String.valueOf(generatedCardId)); // 将卡号转为字符串类型
                        acc.setMoney(acc.getMoney());
                        System.out.println("DEBUG: 执行到这里，卡号是：" + acc.getCardId());
                        System.out.println("DEBUG: 余额是：" + acc.getMoney());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("插入用户信息失败，事务回滚");
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 从数据库中读取用户信息并初始化账户集合
    private void loadAccountsFromDatabase() {
        try (Connection connection = createConnection()) {
            String sql = "SELECT * FROM ATM_user";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Account acc = new Account();
                        acc.setUsername(resultSet.getString("Username"));
                        acc.setCardId(resultSet.getString("cardId"));
                        acc.setSex(resultSet.getString("sex").charAt(0));
                        acc.setPassWord(resultSet.getString("passWord"));
                        acc.setMoney(resultSet.getDouble("money"));
                        acc.setLimit(resultSet.getDouble("limit"));
                        accounts.add(acc);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 检查数据库中是否已存在相同的卡号
    private boolean isCardIdUnique(String cardId, Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ATM_user WHERE cardId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, cardId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    System.out.println("DEBUG: 卡号 " + cardId + " 在数据库中的数量为：" + count);
                    return count == 0; // 如果查询结果为0，则表示卡号唯一
                }
            }
        }
        return false; // 出现异常时也返回false，表示不唯一
    }

    // 初始化数据库连接
    public ATM() {

        try {
            createConnection(); // 调用创建数据库连接的方法
            // 调用加载账户信息的方法
            loadAccountsFromDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            // 处理数据库连接异常
        }
    }

    // 关闭数据库连接
    private void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 启动ATM系统，展示欢迎界面

    public void start() {
        loadAccountsFromDatabase(); // 手动加载账户信息

        while (true) {
            System.out.println("===欢迎您进入ATM系统===");
            System.out.println("1.用户登录");
            System.out.println("2.用户开户");
            System.out.println("请选择：");
            int command = sc.nextInt();
            switch (command) {
                case 1:
                    // 用户登录
                    login();
                    break;
                case 2:
                    // 用户开户
                    createAccount();
                    break;
                default:
                    System.out.println("没有该操作");
            }
        }
    }

    // 完成用户的登录操作
    private void login() {
        System.out.println("===系统登陆===");
        // 1.判断系统中是否存在账户对象，存在才能登录，如果不存在，我们直接结束登录操作
        if (accounts.size() == 0) {
            System.out.println("当前系统中无任何账户，请先开户，再登录");
            return; // 跳出登录操作
        }
        // 2.系统中存在账户对象，可以开始进行登录操作了
        while (true) {
            System.out.println("请您输入您的登录卡号：");
            String cardId = sc.next();
            // 3.判断卡号是否存在
            Account acc = getAccountByCardId(cardId);
            if (acc == null) {
                // 说明这个卡号不存在
                System.out.println("卡号不存在，请重新输入");
                ;
            } else {
                while (true) {
                    // 卡号存在了，接着让用户输入密码
                    System.out.println("请您输入您的登录密码：");
                    String passWord = sc.next();
                    // 4.判断密码是否正确
                    if (acc.getPassWord().equals(passWord)) {
                        loginAcc = acc;
                        // 密码正确，登录成功
                        System.out.println("恭喜您：" + acc.getUsername() + "成功登录系统，您的卡号是：" + acc.getCardId());
                        // 展示登录后的操作界面
                        showUserCommand();
                        return; // 跳出并结束当前登录方法
                    } else {
                        System.out.println("您输入的密码不正确，请重新输入");
                    }
                }
            }
        }
    }

    // 展示登录后的操作界面
    private void showUserCommand() {
        while (true) {
            System.out.println(loginAcc.getUsername() + "您可以选择如下功能进行账户的处理===");
            System.out.println("1.查询账户");
            System.out.println("2.存款");
            System.out.println("3.取款");
            System.out.println("4.转账");
            System.out.println("5.密码修改");
            System.out.println("6.退出");
            System.out.println("7.注销当前账户");
            System.out.println("请选择：");
            int command = sc.nextInt();
            switch (command) {
                case 1:
                    // 查询当前账户
                    showLoginAcc();
                    break;
                case 2:
                    // 存款
                    depositMoney();
                    break;
                case 3:
                    // 取款
                    drawMoney();
                    break;
                case 4:
                    // 转账
                    transferMoney();
                    break;
                case 5:
                    // 密码修改
                    updatePassword();
                    // 在 updatePassword 方法中已经更新了密码，接下来将更新后的密码同步到数据库
                    updatePasswordInDatabase(loginAcc);
                    return;
                case 6:
                    // 退出
                    System.out.println(loginAcc.getUsername() + "您已退出账户");
                    return; // 跳出并结束当前方法
                case 7:
                    // 注销当前登录账户
                    if (deleteAccount()) {
                        // 销户成功，回到欢迎界面
                        return;
                    }
                    break;
                default:
                    System.out.println("您当前选择的操作是不存在的，请重新选择");
            }

            // 插入用户信息到数据库，移至此处确保在操作结束后执行
            insertUserToDatabase(loginAcc);
        }
    }

    // 更新用户密码
    private void updatePasswordInDatabase(Account acc) {
        try (Connection connection = createConnection()) {
            String sql = "UPDATE ATM_user SET passWord = ? WHERE cardId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, acc.getPassWord());
                preparedStatement.setString(2, acc.getCardId());

                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 账户密码修改
    private void updatePassword() {
        System.out.println("==修改密码==");
        // 1.提醒用户认证当前密码
        while (true) {
            System.out.println("请输入当前密码：");
            String passWord = sc.next();

            // 2.认证当前密码是否正确
            if (loginAcc.getPassWord().equals(passWord)) {
                // 认证通过
                while (true) {
                    // 3.真正开始修改密码
                    System.out.println("请输入新密码：");
                    String newPassWord = sc.next();

                    System.out.println("请输入确认密码：");
                    String okPassWord = sc.next();

                    // 4.判断两次密码是否一致
                    if (newPassWord.equals(okPassWord)) {
                        // 可以真正修改密码了
                        loginAcc.setPassWord(newPassWord);
                        System.out.println("密码修改成功，请重新登录");
                        // 插入用户信息到数据库
                        insertUserToDatabase(loginAcc);
                        return;
                    } else {
                        System.out.println("您输入的两次密码不一致~~");
                    }
                }
            } else {
                System.out.println("您当前输入的密码不正确~~~");
            }
        }
    }

    // 注销账户
    private boolean deleteAccount() {
        System.out.println("==进行销户操作==");
        // 1.问问用户是否确定销户
        System.out.println("请问您确定要销户吗？y/n");
        String command = sc.next();
        switch (command) {
            case "y":
                // 销户
                // 2.判断账户中是否有钱？ loginAcc
                if (loginAcc.getMoney() == 0) {
                    // 真的销户
                    // 插入用户信息到数据库
                    insertUserToDatabase(loginAcc);

                    // 从数据库中删除账户信息
                    deleteAccountFromDatabase(loginAcc.getCardId());

                    // 从集合中移除账户
                    accounts.remove(loginAcc);
                    System.out.println("销户成功");
                    return true;
                } else {
                    System.out.println("对不起，您的账户中存在余额，请先结清账户");
                    return false;
                }
            case "n":
                System.out.println("您已取消销户");
                return false;
            default:
                System.out.println("您当前选择的操作是不存在的，请重新选择");
                return false;
        }
    }

    // 从数据库中删除账户信息
    private void deleteAccountFromDatabase(String cardId) {
        Connection connection = null;

        try {
            connection = createConnection();
            connection.setAutoCommit(false);

            String sql = "DELETE FROM ATM_user WHERE cardId = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, cardId);
                preparedStatement.executeUpdate();

                connection.commit();
            }
        } catch (SQLException e) {
            // 处理异常
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            // 关闭连接等资源
            closeConnection(connection);
        }
    }

    // 转账
    private void transferMoney() {
        System.out.println("==用户转账==");
        // 1.判断系统中是否有其他账户
        if (accounts.size() < 2) {
            System.out.println("当前系统中没有其他账户，无法转账");
            return;
        }

        // 2.判断自己账户中是否有钱
        if (loginAcc.getMoney() == 0) {
            System.out.println("您的账户中没有存款，无法转账");
            return;
        }

        // 3.真正开始转账了
        while (true) {
            System.out.println("请您输入对方卡号：");
            String cardId = sc.next();

            // 4.判断这个卡号是否正确
            Account acc = getAccountByCardId(cardId);
            if (acc == null) {
                System.out.println("您输入的卡号有误，请重新输入");
            } else {
                // 对方账户存在，继续让用户认证姓氏
                String name = "*" + acc.getUsername().substring(1);
                System.out.println("请您输入[" + name + "]姓氏");
                String preName = sc.next();
                // 5.判断这个姓氏是否正确
                if (acc.getUsername().startsWith(preName)) {
                    while (true) {
                        // 认证通过了，可以开始转账
                        System.out.println("请您输入转账给对方的金额：");
                        double money = sc.nextDouble();
                        // 6.判断这个金额是否超过自己的余额
                        if (loginAcc.getMoney() >= money) {
                            // 转账成功
                            // 更新自己的账户余额
                            loginAcc.setMoney(loginAcc.getMoney() - money);
                            // 更新对方的账户余额
                            acc.setMoney(acc.getMoney() + money);

                            System.out.println("您转账成功了~~~~");

                            // 插入用户信息到数据库
                            insertUserToDatabase(loginAcc);
                            updateUserInDatabase(acc); // 立即同步对方账户的信息到数据库

                            // 更新用户信息到数据库
                            updateUserInDatabase(loginAcc);

                            // 输出调试信息
                            System.out.println("存款后数据库中账户余额：" + getUserBalanceFromDatabase(loginAcc.getCardId()));

                            return; // 直接跳出转账方法
                        } else {
                            System.out.println("您余额不足，无法给对方转这么多钱，最多可转：" + loginAcc.getMoney());
                        }
                    }
                } else {
                    System.out.println("对不起，您认证的姓氏有问题~~");
                }
            }
        }
    }

    // 取钱
    private void drawMoney() {
        System.out.println("==取钱操作==");
        if (this.loginAcc.getMoney() < 100.0) {
            System.out.println("您的账户余额不足100元，无法取钱，请先充值");
        } else {
            while (true) {
                System.out.println("请您输入取款金额：");

                // 检查输入是否为整数
                if (this.sc.hasNextInt()) {
                    int money = this.sc.nextInt();

                    // 检查取款金额是否合法
                    if (money <= 0) {
                        System.out.println("取款金额必须大于零，请重新输入");
                        continue;
                    }

                    // 检查是否为100的整数倍
                    if (money % 100 == 0) {
                        // 检查账户余额是否足够
                        if (this.loginAcc.getMoney() >= money) {
                            // 检查取款金额是否超过每次限额
                            if (!(money > this.loginAcc.getLimit())) {
                                this.loginAcc.setMoney(this.loginAcc.getMoney() - money);
                                System.out.println("您取款：" + money + "成功，取款后剩余：" + this.loginAcc.getMoney() + "元");

                                // 插入用户信息到数据库
                                insertUserToDatabase(loginAcc);

                                // 更新用户信息到数据库
                                updateUserInDatabase(loginAcc);

                                return;
                            } else {
                                System.out.println("您当前取款金额超过了每次限额，您每次最多可取：" + this.loginAcc.getLimit() + "元");
                            }
                        } else {
                            System.out.println("余额不足，您的账户中的余额是：" + this.loginAcc.getMoney() + "元");
                        }
                    } else {
                        System.out.println("对不起，取款金额必须为100的整数倍~~");
                    }
                } else {
                    System.out.println("输入的不是整数，请重新输入");
                    this.sc.next(); // 清空输入缓冲区
                }
            }
        }
    }

    // 存款操作
    private void depositMoney() {
        System.out.println("==存钱操作==");

        // 输入存款金额
        System.out.println("请您输入存款金额：");
        double money;

        // 检查输入是否为正数
        while (true) {
            if (this.sc.hasNextDouble()) {
                money = this.sc.nextDouble();
                if (money > 0) {
                    break;
                } else {
                    System.out.println("存款金额必须为正数，请重新输入：");
                }
            } else {
                System.out.println("输入的不是数字，请重新输入：");
                this.sc.next(); // 清空输入缓冲区
            }
        }

        // 输出调试信息
        System.out.println("存款金额：" + money);
        System.out.println("当前账户余额：" + this.loginAcc.getMoney());

        // 更新当前登录的账户的余额
        this.loginAcc.setMoney(this.loginAcc.getMoney() + money);
        System.out.println("恭喜您，您存钱：" + money + "成功，当前账户余额为：" + this.loginAcc.getMoney());

        // 更新用户信息到数据库
        updateUserInDatabase(loginAcc);

        // 输出调试信息
        System.out.println("存款后数据库中账户余额：" + getUserBalanceFromDatabase(loginAcc.getCardId()));
    }

    // 更新用户信息到数据库
    private void updateUserInDatabase(Account account) {
        try {
            // 创建连接
            Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);

            // 更新账户余额
            String updateQuery = "UPDATE atm_user SET money = ? WHERE cardId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setDouble(1, account.getMoney());
                preparedStatement.setString(2, account.getCardId());
                preparedStatement.executeUpdate();
            }

            // 关闭连接
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 新增方法，用于从数据库中获取账户余额
    private double getUserBalanceFromDatabase(String cardId) {
        try (Connection connection = createConnection()) {
            String sql = "SELECT money FROM ATM_user WHERE cardId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, cardId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble("money");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // 处理数据库连接异常
        }
        return -1; // 表示获取失败
    }

    // 展示当前登录的账户信息
    private void showLoginAcc() {
        System.out.println("==当前您的账户信息如下==");
        System.out.println("卡号" + loginAcc.getCardId());
        System.out.println("户主" + loginAcc.getUsername());
        System.out.println("性别" + loginAcc.getSex());
        System.out.println("余额" + loginAcc.getMoney());
        System.out.println("每次取现额度" + loginAcc.getLimit());
    }

    // 完成用户开户操作
    private void createAccount() {
        System.out.println("===系统开户操作===");
        // 1.创建一个账户对象，用于封装用户的开户信息
        Account acc = new Account();

        // 2.需要用户输入自己的开户信息，赋值给账号对象
        System.out.println("请输入用户姓名：");
        String name = sc.next();
        acc.setUsername(name);

        while (true) {
            System.out.println("请您输入您的性别");
            char sex = sc.next().charAt(0); // "男"
            if (sex == '男' || sex == '女') {
                acc.setSex(sex);
                break;
            } else {
                System.out.println("您输入的性别有误~只能是男或女~");
            }
        }

        while (true) {
            System.out.println("请您输入您的账号密码：");
            String passWord = sc.next();
            System.out.println("请您输入您的确认密码");
            String okpassWord = sc.next();
            // 2.判断两次密码对象是否一样
            if (passWord.equals(okpassWord)) {
                acc.setPassWord(okpassWord);
                break;
            } else {
                System.out.println("两次密码不一致，请重新输入");
            }
        }

        System.out.println("请您输入您的取现额度：");
        double limit = sc.nextDouble();
        acc.setLimit(limit);

        // 重点：我们需要为这个账号生成一个卡号(有系统自动生成，8位数字表示，不能与其他账户的卡号重复)
        String newCardId = createCardId();
        acc.setCardId(newCardId);
        ;
        // 3.把这个账号对象，存入到账号集合中去
        accounts.add(acc);
        System.out.println("恭喜您：" + acc.getUsername() + "开户成功，您的卡号为：" + acc.getCardId());

        // 插入用户信息到数据库
        insertUserToDatabase(acc);
    }

    // 返回一个8位数字的卡号，而且这个卡号不能与集合中的其他卡号重复

    private String createCardId() {
        // 1.定义一个String类型的变量记住8位数字作为一个卡号
        while (true) {
            String cardId = "";

            // 2.使用循环，循环8次，每次产生一个随机数给cardLd连接起来
            Random r = new Random();
            for (int i = 0; i < 8; i++) {
                int date = r.nextInt(10);
                cardId += date;
            }
            // 3.判断cardId中记住的卡号，是否与其他账户的卡号重复了，没有重复，才可以作为一个新卡号返回。
            if (!isCardIdExist(cardId)) {
                return cardId;
            }
        }
    }

    // 检查生成的卡号是否已经存在于数据库中
    private boolean isCardIdExist(String cardId) {
        try (Connection connection = createConnection()) {
            String sql = "SELECT * FROM ATM_user WHERE cardId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, cardId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSet.next(); // 如果结果集中有数据，说明卡号已存在，返回true；否则，返回false。
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // 处理数据库连接异常
            return true; // 如果发生异常，假设卡号已存在，以避免继续尝试插入相同的卡号。
        }
    }

    // 根据卡号查询账户对象返回,accounts = [c1,c2, c3......]
    private Account getAccountByCardId(String cardId) {
        // 遍历全部的账户对象
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            // 判断这个账户对象acc中的卡号是否是我们要找的卡号
            if (acc.getCardId().equals(cardId)) {
                return acc;
            }
        }
        return null; // 查无此账户
    }
}
