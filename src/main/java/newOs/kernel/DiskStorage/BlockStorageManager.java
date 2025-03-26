package newOs.kernel.DiskStorage;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 块存储管理器，负责 blocks 表的 CRUD 操作
 */
@Component
public class BlockStorageManager {
    private static final String DB_URL = "jdbc:sqlite:src\\main\\java\\newOs\\component\\disk.db";

    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 添加块
     * @param blockNumber 块号（唯一）
     * @param dataJson JSON格式的块数据
     * @param isUsed 是否已使用
     * @param nextBlock 下一个块号
     */
    public void addBlock(int blockNumber, String dataJson, boolean isUsed, int nextBlock) throws SQLException {
        String sql = "INSERT INTO blocks(block_number, data, is_used, next_block) VALUES(?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, blockNumber);
            pstmt.setString(2, dataJson);
            pstmt.setBoolean(3, isUsed);
            pstmt.setInt(4, nextBlock);
            pstmt.executeUpdate();
        }
    }

    /**
     * 根据块号获取块信息
     */
    public Block getBlockByNumber(int blockNumber) throws SQLException {
        String sql = "SELECT * FROM blocks WHERE block_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, blockNumber);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? mapResultSetToBlock(rs) : null;
        }
    }

    /**
     * 更新块数据
     * @param blockNumber 目标块号
     * @param newDataJson 新的JSON数据
     */
    public void updateBlockData(int blockNumber, String newDataJson) throws SQLException {
        String sql = "UPDATE blocks SET data = ? WHERE block_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newDataJson);
            pstmt.setInt(2, blockNumber);
            pstmt.executeUpdate();
        }
    }

    /**
     * 更新块使用状态
     */
    public void updateBlockUsage(int blockNumber, boolean isUsed) throws SQLException {
        String sql = "UPDATE blocks SET is_used = ? WHERE block_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isUsed);
            pstmt.setInt(2, blockNumber);
            pstmt.executeUpdate();
        }
    }

    /**
     * 更新下一个块指针
     */
    public void updateNextBlock(int blockNumber, int newNextBlock) throws SQLException {
        String sql = "UPDATE blocks SET next_block = ? WHERE block_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newNextBlock);
            pstmt.setInt(2, blockNumber);
            pstmt.executeUpdate();
        }
    }

    /**
     * 删除指定块
     */
    public void deleteBlock(int blockNumber) throws SQLException {
        String sql = "DELETE FROM blocks WHERE block_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, blockNumber);
            pstmt.executeUpdate();
        }
    }

    /**
     * 获取所有块列表
     */
    public List<Block> getAllBlocks() throws SQLException {
        List<Block> blocks = new ArrayList<>();
        String sql = "SELECT * FROM blocks";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                blocks.add(mapResultSetToBlock(rs));
            }
        }
        return blocks;
    }

    /**
     * 查找第一个未被使用的块（按块号升序）
     * @return 首个未使用块，若无可用块则返回null
     */
    public Block findFirstUnusedBlock() throws SQLException {
        String sql = "SELECT * FROM blocks WHERE is_used = false ORDER BY block_number ASC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? mapResultSetToBlock(rs) : null;
        }
    }

    public void releaseBlock(List<Integer> blockNumbers) throws SQLException {
        if (blockNumbers == null || blockNumbers.isEmpty()) return;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (int blockNumber : blockNumbers) {
                    updateBlockData(blockNumber, "");
                    updateBlockUsage(blockNumber, false);
                    updateNextBlock(blockNumber, -1);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * 将 ResultSet 映射为 Block 对象
     */
    private Block mapResultSetToBlock(ResultSet rs) throws SQLException {
        return new Block(
                rs.getInt("block_number"),
                rs.getString("data"),
                rs.getBoolean("is_used"),
                rs.getInt("next_block")
        );
    }

    /**
     * 块数据实体类
     */
    @Data
    public static class Block {

        public static final int BLOCK_SIZE = 4096;

        private final int blockNumber;
        private String data;
        private boolean isUsed;
        private int nextBlock;

        public Block(int blockNumber, String data, boolean isUsed, int nextBlock) {
            this.blockNumber = blockNumber;
            this.data = data;
            this.isUsed = isUsed;
            this.nextBlock = nextBlock;
        }

    }
}