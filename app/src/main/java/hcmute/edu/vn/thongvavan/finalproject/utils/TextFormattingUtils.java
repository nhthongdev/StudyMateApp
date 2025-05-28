package hcmute.edu.vn.thongvavan.finalproject.utils;

/**
 * Lớp tiện ích xử lý định dạng văn bản
 */
public class TextFormattingUtils {
    
    /**
     * Giữ nguyên định dạng xuống dòng của văn bản nguồn trong văn bản đích
     * @param sourceText Văn bản nguồn
     * @param translatedText Văn bản đã dịch
     * @return Văn bản đã dịch với định dạng xuống dòng được giữ nguyên
     */
    public static String preserveLineBreaks(String sourceText, String translatedText) {
        if (sourceText == null || translatedText == null) {
            return translatedText;
        }
        
        // Cách tiếp cận đơn giản hơn: tách văn bản nguồn theo dòng
        String[] sourceLines = sourceText.split("\\n");
        
        // Nếu văn bản nguồn chỉ có 1 dòng, trả về văn bản đã dịch
        if (sourceLines.length <= 1) {
            return translatedText;
        }
        
        // Tính toán độ dài trung bình của mỗi dòng trong văn bản nguồn
        int totalSourceChars = 0;
        for (String line : sourceLines) {
            totalSourceChars += line.length();
        }
        float avgSourceLineLength = totalSourceChars / (float) sourceLines.length;
        
        // Tính toán số dòng cần có trong văn bản đã dịch
        float translatedToSourceRatio = translatedText.length() / (float) sourceText.length();
        int estimatedLines = Math.max(sourceLines.length, Math.round(translatedText.length() / avgSourceLineLength));
        
        // Tách văn bản đã dịch thành các đoạn văn có độ dài tương đương
        StringBuilder result = new StringBuilder();
        int charsPerLine = Math.round(translatedText.length() / (float) estimatedLines);
        
        // Đảm bảo charsPerLine không quá nhỏ
        charsPerLine = Math.max(charsPerLine, 10);
        
        int startPos = 0;
        while (startPos < translatedText.length()) {
            int endPos = Math.min(startPos + charsPerLine, translatedText.length());
            
            // Tìm vị trí kết thúc từ gần nhất để tránh cắt giữa từ
            if (endPos < translatedText.length()) {
                while (endPos > startPos && !Character.isWhitespace(translatedText.charAt(endPos))) {
                    endPos--;
                }
                // Nếu không tìm thấy khoảng trắng, sử dụng vị trí ban đầu
                if (endPos == startPos) {
                    endPos = Math.min(startPos + charsPerLine, translatedText.length());
                }
            }
            
            // Thêm đoạn văn bản vào kết quả
            result.append(translatedText.substring(startPos, endPos));
            
            // Thêm xuống dòng nếu không phải đoạn cuối cùng
            if (endPos < translatedText.length()) {
                result.append("\n");
            }
            
            // Cập nhật vị trí bắt đầu cho đoạn tiếp theo
            startPos = endPos;
            // Bỏ qua khoảng trắng ở đầu đoạn tiếp theo
            while (startPos < translatedText.length() && Character.isWhitespace(translatedText.charAt(startPos))) {
                startPos++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Đếm số lần xuống dòng trong văn bản
     * @param text Văn bản cần đếm
     * @return Số lần xuống dòng
     */
    private static int countLineBreaks(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Tìm vị trí xuống dòng trong văn bản
     * @param text Văn bản cần tìm
     * @return Mảng vị trí xuống dòng
     */
    private static int[] findLineBreakPositions(String text) {
        int lineBreakCount = countLineBreaks(text);
        int[] positions = new int[lineBreakCount];
        
        int index = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                positions[index++] = i;
            }
        }
        
        return positions;
    }
    
    /**
     * Tính toán tỷ lệ vị trí xuống dòng trong văn bản
     * @param text Văn bản cần tính toán
     * @param lineBreakPositions Mảng vị trí xuống dòng
     * @return Mảng tỷ lệ vị trí xuống dòng
     */
    private static float[] calculateLineBreakRatios(String text, int[] lineBreakPositions) {
        float textLength = text.length();
        float[] ratios = new float[lineBreakPositions.length];
        
        for (int i = 0; i < lineBreakPositions.length; i++) {
            ratios[i] = lineBreakPositions[i] / textLength;
        }
        
        return ratios;
    }
    
    /**
     * Áp dụng xuống dòng vào văn bản theo tỷ lệ
     * @param text Văn bản cần áp dụng
     * @param lineBreakRatios Mảng tỷ lệ vị trí xuống dòng
     * @return Văn bản đã áp dụng xuống dòng
     */
    private static String applyLineBreaks(String text, float[] lineBreakRatios) {
        StringBuilder result = new StringBuilder(text);
        float textLength = text.length();
        
        // Áp dụng xuống dòng từ cuối lên để không ảnh hưởng đến vị trí
        for (int i = lineBreakRatios.length - 1; i >= 0; i--) {
            int position = Math.round(lineBreakRatios[i] * textLength);
            
            // Đảm bảo vị trí hợp lệ
            if (position >= 0 && position < result.length()) {
                // Tìm vị trí kết thúc từ gần nhất
                int insertPosition = findWordBoundary(result.toString(), position);
                result.insert(insertPosition, '\n');
            }
        }
        
        return result.toString();
    }
    
    /**
     * Tìm vị trí kết thúc từ gần nhất
     * @param text Văn bản cần tìm
     * @param position Vị trí bắt đầu tìm
     * @return Vị trí kết thúc từ gần nhất
     */
    private static int findWordBoundary(String text, int position) {
        // Nếu vị trí đã là khoảng trắng, trả về vị trí đó
        if (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            return position;
        }
        
        // Tìm khoảng trắng tiếp theo
        int nextSpace = text.indexOf(' ', position);
        
        // Nếu không tìm thấy khoảng trắng, trả về vị trí ban đầu
        if (nextSpace == -1) {
            return position;
        }
        
        return nextSpace;
    }
    
    /**
     * Phương thức để giữ nguyên cấu trúc dòng của văn bản nguồn trong văn bản đã dịch
     * Dịch từng dòng riêng biệt và giữ nguyên cấu trúc dòng
     * @param sourceText Văn bản nguồn
     * @param translatedText Văn bản đã dịch
     * @return Văn bản đã dịch với cấu trúc dòng được giữ nguyên
     */
    public static String preserveLineBreaksSimple(String sourceText, String translatedText) {
        if (sourceText == null || translatedText == null) {
            return translatedText;
        }
        
        // Nếu văn bản nguồn không có xuống dòng, trả về văn bản đã dịch
        if (!sourceText.contains("\n")) {
            return translatedText;
        }
        
        try {
            // Tách văn bản nguồn thành các dòng
            String[] sourceLines = sourceText.split("\n", -1); // -1 để giữ cả các dòng trống
            
            // Gọi API dịch riêng cho từng dòng sẽ phức tạp, nên chúng ta sẽ chia văn bản đã dịch
            // thành các phần tương ứng với các dòng trong văn bản nguồn
            
            // Tính toán độ dài trung bình của mỗi dòng trong văn bản đã dịch
            float avgTranslatedLineLength = (float) translatedText.length() / sourceLines.length;
            
            // Tạo mảng chứa độ dài của mỗi dòng trong văn bản nguồn
            float[] sourceLineLengths = new float[sourceLines.length];
            float totalSourceLength = 0;
            
            for (int i = 0; i < sourceLines.length; i++) {
                sourceLineLengths[i] = sourceLines[i].length();
                totalSourceLength += sourceLineLengths[i];
            }
            
            // Tính tỷ lệ độ dài của mỗi dòng so với tổng độ dài
            float[] sourceLineRatios = new float[sourceLines.length];
            for (int i = 0; i < sourceLines.length; i++) {
                if (totalSourceLength > 0) {
                    sourceLineRatios[i] = sourceLineLengths[i] / totalSourceLength;
                } else {
                    // Nếu tổng độ dài là 0, phân bổ đều
                    sourceLineRatios[i] = 1.0f / sourceLines.length;
                }
            }
            
            // Tính toán độ dài của mỗi dòng trong văn bản đã dịch dựa trên tỷ lệ
            int[] translatedLineLengths = new int[sourceLines.length];
            int remainingLength = translatedText.length();
            
            for (int i = 0; i < sourceLines.length - 1; i++) {
                translatedLineLengths[i] = Math.round(translatedText.length() * sourceLineRatios[i]);
                remainingLength -= translatedLineLengths[i];
            }
            
            // Độ dài của dòng cuối cùng là phần còn lại
            translatedLineLengths[sourceLines.length - 1] = remainingLength;
            
            // Tách văn bản đã dịch thành các dòng dựa trên độ dài đã tính toán
            String[] translatedLines = new String[sourceLines.length];
            int startIndex = 0;
            
            for (int i = 0; i < sourceLines.length; i++) {
                int endIndex = Math.min(startIndex + translatedLineLengths[i], translatedText.length());
                
                if (startIndex < translatedText.length()) {
                    translatedLines[i] = translatedText.substring(startIndex, endIndex);
                } else {
                    translatedLines[i] = "";
                }
                
                startIndex = endIndex;
            }
            
            // Ghép các dòng lại với nhau, thêm ký tự xuống dòng
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < translatedLines.length; i++) {
                result.append(translatedLines[i]);
                
                // Thêm xuống dòng nếu không phải dòng cuối cùng
                if (i < translatedLines.length - 1) {
                    result.append("\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            // Nếu có lỗi, trả về văn bản đã dịch gốc
            return translatedText;
        }
    }
    
    /**
     * Tách văn bản đã dịch thành các đoạn tương ứng với số dòng trong văn bản nguồn
     * @param translatedText Văn bản đã dịch
     * @param lineCount Số dòng trong văn bản nguồn
     * @return Mảng các đoạn văn bản đã dịch
     */
    private static String[] splitTranslatedText(String translatedText, int lineCount) {
        // Nếu chỉ có một dòng, trả về văn bản đã dịch
        if (lineCount <= 1) {
            return new String[] { translatedText };
        }
        
        String[] result = new String[lineCount];
        
        // Tính độ dài trung bình của mỗi đoạn
        int avgLength = translatedText.length() / lineCount;
        
        // Tách văn bản thành các đoạn có độ dài tương đương nhau
        int startIndex = 0;
        for (int i = 0; i < lineCount - 1; i++) {
            int endIndex = startIndex + avgLength;
            
            // Tìm vị trí kết thúc từ gần nhất
            if (endIndex < translatedText.length()) {
                endIndex = findWordBoundary(translatedText, endIndex);
            } else {
                endIndex = translatedText.length();
            }
            
            // Lấy đoạn văn bản
            result[i] = translatedText.substring(startIndex, endIndex);
            
            // Cập nhật vị trí bắt đầu cho đoạn tiếp theo
            startIndex = endIndex;
        }
        
        // Đoạn cuối cùng là phần còn lại của văn bản
        result[lineCount - 1] = translatedText.substring(startIndex);
        
        return result;
    }
}
