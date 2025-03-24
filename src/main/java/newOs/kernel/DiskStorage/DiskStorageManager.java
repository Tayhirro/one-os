package newOs.kernel.DiskStorage;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/**
 * 磁盘存储管理器，通过SQLite数据库模拟磁盘块存储。
 * 提供块分配、释放、读写等核心功能，块大小固定为4KB。
 */
@Component
@Data
public class DiskStorageManager {

    //这个地方写的不行，拉到properties去配置
    //
    //
    //
    //
    private static final String DB_URL = "jdbc:sqlite:disk.db";
    private static final int BLOCK_SIZE = 4096; // 4KB块大小
    //
    //
    //
    //


    //管理配置修改mapper，或者能用也行
    /** 获取数据库连接 */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 分配指定数量的空闲块
     * @param numBlocks 需分配的块数（需>0）
     * @return 分配的块号列表
     * @throws SQLException 若数据库错误或块不足
     */
    public List<Integer> allocateBlocks(int numBlocks) throws SQLException {
        List<Integer> allocatedBlocks = new ArrayList<>();
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // 查询并锁定空闲块
            PreparedStatement selectStmt = conn.prepareStatement(
                    "SELECT block_number FROM blocks WHERE is_used = FALSE LIMIT ?"
            );
            selectStmt.setInt(1, numBlocks);
            ResultSet rs = selectStmt.executeQuery();

            // 收集可用块号
            while (rs.next()) {
                int blockNum = rs.getInt("block_number");
                allocatedBlocks.add(blockNum);
            }

            if (allocatedBlocks.size() < numBlocks) {
                conn.rollback();
                throw new SQLException("Not enough free blocks");
            }

            // 标记块为已用
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE blocks SET is_used = TRUE WHERE block_number = ?"
            );
            for (int blockNum : allocatedBlocks) {
                updateStmt.setInt(1, blockNum);
                updateStmt.addBatch();
            }
            updateStmt.executeBatch();
            conn.commit();
        }
        return allocatedBlocks;
    }

    /**
     * 释放块，标记为空闲状态
     * @param blockNumbers 需释放的块号列表
     * @throws SQLException 若数据库错误
     */
    public void freeBlocks(List<Integer> blockNumbers) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE blocks SET is_used = FALSE WHERE block_number = ?"
            );
            for (int blockNum : blockNumbers) {
                stmt.setInt(1, blockNum);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * 向指定块写入数据
     * @param blockNumber 块号（必须已分配）
     * @param data 数据（长度不得超过4096字节）
     * @throws SQLException 若数据库错误
     * @throws IllegalArgumentException 若数据长度超过块容量
     */
    public void writeBlock(int blockNumber, byte[] data) throws SQLException {
        if (data.length > BLOCK_SIZE) {
            throw new IllegalArgumentException("数据长度超过块容量（4096字节）");
        }

        try (Connection conn = getConnection()) {
            // 直接写入原始数据（不需要填充）
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE blocks SET data = ? WHERE block_number = ?"
            );
            stmt.setBytes(1, data);
            stmt.setInt(2, blockNumber);
            stmt.executeUpdate();
        }
    }


    /**
     * 从指定块读取有效数据（实际写入长度，可能小于4096字节）
     * @param blockNumber 块号（必须已分配）
     * @return 块中的有效数据（实际长度），不存在返回null
     * @throws SQLException 若数据库错误
     */
    public byte[] readBlock(int blockNumber) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT data FROM blocks WHERE block_number = ?"
            );
            stmt.setInt(1, blockNumber);
            ResultSet rs = stmt.executeQuery();

            // 直接返回原始数据（长度<=4096）
            return rs.next() ? rs.getBytes("data") : null;
        }
    }
    /*
    // 获取文件的块号列表（需在文件表中记录）
    public List<Integer> getFileBlocks(String fileId) throws SQLException {
        List<Integer> blocks = new ArrayList<>();
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT block_number FROM file_blocks WHERE file_id = ? ORDER BY block_order"
            );
            stmt.setString(1, fileId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                blocks.add(rs.getInt("block_number"));
            }
        }
        return blocks;
    }
*/
}