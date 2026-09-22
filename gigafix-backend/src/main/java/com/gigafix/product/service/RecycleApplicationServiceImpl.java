package com.gigafix.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gigafix.common.config.CacheConfig;
import com.gigafix.member.entity.Member;
import com.gigafix.member.exception.MemberNotFoundException;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.product.Utils;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.ProductSaleStatus;
import com.gigafix.product.constant.RecycleStatus;
import com.gigafix.product.dto.RecycleAgreementRequest;
import com.gigafix.product.dto.RecycleApplicationImportItem;
import com.gigafix.product.dto.RecycleQueryParams;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.entity.Product;
import com.gigafix.product.repository.ProductDao;
import com.gigafix.product.repository.RecycleApplicationDao;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.StoresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Transactional
@Service
public class RecycleApplicationServiceImpl implements RecycleApplicationService{
    private static final DateTimeFormatter EXCEL_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final String AGREEMENT_OTP_KEY_PREFIX = "recycle-agreement:";
    private static final String PNG_DATA_URL_PREFIX = "data:image/png;base64,";
    private static final Pattern CONDITION_PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
    private static final Pattern CONDITION_CHENG_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*成新");

    @Autowired
    private RecycleApplicationDao recycleApplicationDao;
    @Autowired
    private ProductDao productDao;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private StoresRepository storesRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RecycleApplicationNotificationService recycleApplicationNotificationService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private Cloudinary cloudinary;

    //實作查詢回收單列表

    @Override
    public Page<RecycleResponse> getApplyForms(RecycleQueryParams recycleQueryParams) {
        // 後台可以使用查詢參數中的 memberId
        return queryApplyForms(
                recycleQueryParams.getMemberId(),
                recycleQueryParams
        );
    }
    //實作以會員id查詢回收單列表
    @Override
    public Page<RecycleResponse> getMemberApplyForms(Long memberId, RecycleQueryParams recycleQueryParams) {
        //前台強制使用登入會員的Id
        return queryApplyForms(memberId, recycleQueryParams);
    }

    private Page<RecycleResponse> queryApplyForms(Long memberId, RecycleQueryParams recycleQueryParams) {
        // applyId 由後台搜尋欄位傳入；會員列表通常為 null，仍會由 memberId 限制資料範圍。
        Long applyId = recycleQueryParams.getApplyId();
        String productName = Utils.blankToNull(recycleQueryParams.getProductName());
        String appeareance = Utils.blankToNull(recycleQueryParams.getAppearance());
        String orderBy = Utils.blankToNull(recycleQueryParams.getOrderBy());
        String sortParam = Utils.blankToNull(recycleQueryParams.getSort());
        ProductCategory category = recycleQueryParams.getProductCategory();
        RecycleStatus recycleStatus = recycleQueryParams.getRecycleStatus();
        Integer limit = recycleQueryParams.getLimit();
        Integer offset = recycleQueryParams.getOffset();

        if (limit == null) {
            limit = 20;
        }
        if(offset == null){
            offset = 0;
        }

        if(orderBy == null){
           orderBy = "createdTime"; // 預設依建立時間排序
        }
        if(sortParam == null){
           sortParam = "desc"; // 預設降冪（從新到舊） "desc"字串到時候寫在前端
        }

        //JPA的Sort 物件判斷是.asc().desc()
        Sort sort = sortParam.equalsIgnoreCase("asc") ? //acs字串到時候寫在前端
                Sort.by(orderBy).ascending() ://昇羃
                Sort.by(orderBy).descending();//降冪
        //轉化為jpa頁數
        int page = offset / limit;
        //結合為Pageable物件  參數為 頁數 ,pagesize, 排序
        Pageable pageable = PageRequest.of(page,limit,sort);
        Page<RecycleApplication> applyFormPage  = recycleApplicationDao.findByConditions(applyId, memberId, productName, appeareance, category, recycleStatus, pageable);
        //利用 .map() 把裡面的每一筆 Entity 轉成 DTO，這時型態會自動變成 Page<RecycleResponse>
        Page<RecycleResponse> applyFormList = applyFormPage.map(this::toResponse);

        return applyFormList;
    }

    //實作查詢單筆回收單id
    @Override
    public RecycleResponse getApplyFormById(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao.findById(applyId).orElse(null);

        if(applyForm == null){
            return null;
        }

        return toResponse(applyForm);

    }
    //用作將entity資訊填入RecycleResponse DTO類別 給查詢回收單id用
    private RecycleResponse toResponse(RecycleApplication applyForm){
        RecycleResponse response = new RecycleResponse();

        response.setApplyId(applyForm.getApplyId());
        response.setProductName(applyForm.getProductName());
        response.setCategory(applyForm.getCategory());
        response.setAppearance(applyForm.getAppearance());
        response.setImageUrl(applyForm.getImageUrl());
        response.setDescription(applyForm.getDescription());
        response.setEstimatedPrice(applyForm.getEstimatedPrice());
        response.setRecycleStatus(applyForm.getRecycleStatus());
        response.setAgreementSignedTime(applyForm.getAgreementSignedTime());
        response.setCreatedTime(applyForm.getCreatedTime());
        response.setLastModifiedTime(applyForm.getLastModifiedTime());

        if(applyForm.getMember() !=null){
            response.setMemberId(applyForm.getMember().getId());
            response.setMemberName(applyForm.getMember().getRealName());
            response.setContactPhone(applyForm.getMember().getPhone());
        }

        if(applyForm.getStores() != null){
            response.setStoreId(applyForm.getStores().getId());
            response.setStoreName(applyForm.getStores().getName());
        }

        return response;
    }

    //實作前台以會員id查詢回收單列表

    @Override
    public RecycleResponse getMemberApplyFormById(Long memberId, Long applyId) {
        RecycleApplication applyForm =
                recycleApplicationDao
                        .findByApplyIdAndMember_Id(applyId, memberId)
                        .orElse(null);

        if (applyForm == null) {
            return null;
        }
        return toResponse(applyForm);
    }

    //實作新增回收單
    @Override
    public RecycleResponse createApplyForm(Long memberId, RecycleRequest recycleRequest) {
        RecycleApplication applyForm = new RecycleApplication();

        applyForm.setProductName(recycleRequest.getProductName());
        applyForm.setCategory(recycleRequest.getCategory());
        applyForm.setAppearance(recycleRequest.getAppearance());
        // 會員新增回收單時不接受照片，照片僅能由後台編輯時補上。
        applyForm.setImageUrl(null);
        applyForm.setDescription(recycleRequest.getDescription());
        applyForm.setEstimatedPrice(recycleRequest.getEstimatedPrice());

        applyForm.setRecycleStatus(RecycleStatus.APPLIED);
        applyForm.setCreatedTime(LocalDateTime.now());
        applyForm.setLastModifiedTime(LocalDateTime.now());

        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        applyForm.setMember(member);

        if(recycleRequest.getStoreId()!=null){
            Stores stores = storesRepository.findById(recycleRequest.getStoreId()).orElse(null);
            if(stores == null){
                return null;
            }

            applyForm.setStores(stores);
        }

        RecycleApplication savedApplyForm = recycleApplicationDao.save(applyForm);
        // 必須先儲存取得申請單號，成功信才能帶入完整的回收單資訊。
        recycleApplicationNotificationService.sendApplicationSuccess(savedApplyForm);
        return toResponse(savedApplyForm);
    }

    /*
     * 回收單狀態流程的 Service 實作區。
     *
     * 這個類別透過 implements RecycleApplicationService 實作狀態操作介面。
     * 下方標示 @Override 的方法都對應到 RecycleApplicationService 宣告的功能，
     * Controller 只需要依賴介面，不必知道資料庫、快取或寄信的實作細節。
     *
     * 本段流程使用的主要介面與元件：
     * 1. RecycleApplicationDao：繼承 Spring Data JPA 的
     *    JpaRepository<RecycleApplication, Long>，提供查詢、儲存及悲觀寫入鎖。
     * 2. ProductDao：繼承 JpaRepository<Product, Long>，
     *    回收完成時建立可販售的商品庫存。
     * 3. CacheManager 與 Cache：Spring Cache 介面，
     *    用來保存五分鐘有效且只能成功使用一次的 OTP。
     * 4. RecycleApplicationNotificationService：負責回收通知郵件，
     *    其內部再使用 Spring 的 JavaMailSender 介面寄信。
     * 5. @Transactional：套用在本 Service 類別，
     *    讓狀態、回收單及商品庫存異動在同一交易中提交或回滾。
     *
     * 主要狀態順序為：APPLIED → INSPECTING → WAITING_FOR_AGREEMENT → WIPING → COMPLETED；
     * APPLIED、INSPECTING 或 WAITING_FOR_AGREEMENT 階段可以取消為 CANCELLED。
     */

    /**
     * 實作 {@link RecycleApplicationService#markAsInspecting(Long)}。
     * 使用 {@link RecycleApplicationDao} 的 JpaRepository 查詢與儲存功能，
     * 將已預約交件（APPLIED）的回收單更新為現場檢測中（INSPECTING）。
     */
    @Override
    public RecycleResponse markAsInspecting(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao.findById(applyId).orElse(null);

        if (applyForm == null) {
            return null;
        }

        // 流程只能向前推進，避免重複操作或從其他階段跳回檢測中。
        if (applyForm.getRecycleStatus() != RecycleStatus.APPLIED) {
            throw new IllegalStateException("只有已預約交件的回收單可以進入現場檢測評估中");
        }

        applyForm.setRecycleStatus(RecycleStatus.INSPECTING);
        // 讓前端可顯示這次狀態轉換的實際時間。
        applyForm.setLastModifiedTime(LocalDateTime.now());

        RecycleApplication updatedApplyForm = recycleApplicationDao.save(applyForm);
        return toResponse(updatedApplyForm);
    }

    /**
     * 實作 {@link RecycleApplicationService#sendAgreementOtp(Long)}。
     * 先透過 {@link RecycleApplicationDao} 讀取回收單並檢查狀態與估價，
     * 再由 {@link RecycleApplicationNotificationService} 寄信，最後透過
     * {@link CacheManager} 取得的 {@link Cache} 保存 OTP 與錯誤嘗試次數。
     */
    @Override
    public boolean sendAgreementOtp(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao.findById(applyId).orElse(null);
        if (applyForm == null) {
            return false;
        }

        // 完成現場檢測且已有估價後才可寄送；待簽署階段允許在 OTP 逾時後重寄。
        RecycleStatus currentStatus = applyForm.getRecycleStatus();
        if (currentStatus != RecycleStatus.INSPECTING
                && currentStatus != RecycleStatus.WAITING_FOR_AGREEMENT) {
            throw new IllegalStateException("只有現場檢測或待簽署同意的回收單可以寄送同意驗證碼");
        }
        if (applyForm.getEstimatedPrice() == null) {
            throw new IllegalStateException("請先完成回收估價再寄送同意驗證碼");
        }

        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        // OTP 寄出後即代表檢測與估價皆已完成，正式進入等待會員簽署階段。
        applyForm.setRecycleStatus(RecycleStatus.WAITING_FOR_AGREEMENT);
        applyForm.setLastModifiedTime(LocalDateTime.now());
        recycleApplicationNotificationService.sendAgreementOtp(applyForm, otp);

        // 沿用組長設定的 5 分鐘 Caffeine 快取，以前綴隔離註冊 OTP 與回收同意 OTP。
        getOtpCache().put(agreementOtpKey(applyId), new AgreementOtpEntry(otp));
        recycleApplicationDao.save(applyForm);
        return true;
    }

    /**
     * 實作 {@link RecycleApplicationService#confirmAgreement(Long, Long, RecycleAgreementRequest)}。
     * 使用 Repository 方法同時比對回收單與會員 ID，避免會員操作別人的資料；
     * OTP 與 Canvas PNG 簽名驗證成功後，保存簽名並將狀態推進至 WIPING。
     */
    @Override
    public RecycleResponse confirmAgreement(
            Long memberId,
            Long applyId,
            RecycleAgreementRequest request
    ) {
        // 必須同時符合回收單 ID 與登入會員 ID，前台不能簽署其他會員的回收單。
        RecycleApplication applyForm = recycleApplicationDao
                .findByApplyIdAndMember_Id(applyId, memberId)
                .orElse(null);
        if (applyForm == null) {
            return null;
        }

        if (applyForm.getRecycleStatus() != RecycleStatus.WAITING_FOR_AGREEMENT) {
            throw new IllegalStateException("只有待簽署同意的回收單可以確認估價同意");
        }

        validateSignature(request.getSignatureDataUrl());
        verifyAgreementOtp(applyId, request.getOtp());

        LocalDateTime signedTime = LocalDateTime.now();
        // 驗證成功後先保存 Canvas 簽名與時間，讓回收單保有可追溯的簽署紀錄。
        applyForm.setAgreementSignature(request.getSignatureDataUrl());
        applyForm.setAgreementSignedTime(signedTime);
        // 依目前流程，簽名存檔完成即通知後台開始清除資料，因此直接推進到狀態 4。
        applyForm.setRecycleStatus(RecycleStatus.WIPING);
        applyForm.setLastModifiedTime(signedTime);
        return toResponse(recycleApplicationDao.save(applyForm));
    }

    /**
     * 實作 {@link RecycleApplicationService#completeRecycle(Long)}。
     * {@link RecycleApplicationDao#findByIdForUpdate(Long)} 使用 JPA 悲觀寫入鎖，
     * 避免同一張回收單被同時結案而重複建立庫存；接著透過
     * {@link ProductDao} 新增商品，最後寄送完成回收通知。
     */
    @Override
    public RecycleResponse completeRecycle(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao
                .findByIdForUpdate(applyId)
                .orElse(null);
        if (applyForm == null) {
            return null;
        }
        if (applyForm.getRecycleStatus() != RecycleStatus.WIPING) {
            throw new IllegalStateException("只有資料清除中的回收單可以完成回收");
        }
        if (applyForm.getEstimatedPrice() == null || applyForm.getEstimatedPrice() < 0) {
            throw new IllegalArgumentException("回收單缺少有效估價，無法建立庫存商品");
        }

        LocalDateTime completedTime = LocalDateTime.now();
        Product inventoryProduct = buildInventoryProduct(applyForm, completedTime);
        Product savedInventoryProduct = productDao.save(inventoryProduct);

        // 庫存新增成功後才更新回收單狀態，兩者由同一個交易一併提交或回滾。
        // 關聯由後端自動建立，不接受 RecycleRequest 傳入商品 ID。
        applyForm.setProduct(savedInventoryProduct);
        applyForm.setRecycleStatus(RecycleStatus.COMPLETED);
        applyForm.setLastModifiedTime(completedTime);
        RecycleApplication completedApplication = recycleApplicationDao.save(applyForm);

        // 郵件寄送失敗會向外拋錯，讓結案交易回滾，避免會員未收到結案通知。
        recycleApplicationNotificationService.sendCompletionNotice(completedApplication);
        return toResponse(completedApplication);
    }

    /**
     * 實作後台取消介面 {@link RecycleApplicationService#cancelRecycle(Long)}。
     * 透過悲觀寫入鎖取得回收單，再交由共用方法檢查可取消狀態並更新資料。
     */
    @Override
    public RecycleResponse cancelRecycle(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao
                .findByIdForUpdate(applyId)
                .orElse(null);
        return applyForm == null ? null : cancelApplication(applyForm);
    }

    /**
     * 實作會員取消介面 {@link RecycleApplicationService#cancelMemberRecycle(Long, Long)}。
     * Repository 查詢會同時限制會員 ID 並鎖定資料，確保會員只能取消自己的回收單。
     */
    @Override
    public RecycleResponse cancelMemberRecycle(Long memberId, Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao
                .findMemberApplicationForUpdate(applyId, memberId)
                .orElse(null);
        return applyForm == null ? null : cancelApplication(applyForm);
    }

    /**
     * 後台與會員取消功能共用的狀態轉換實作。
     * APPLIED、INSPECTING 或 WAITING_FOR_AGREEMENT 可以轉為 CANCELLED；
     * 開始清除資料後不可逆轉，同時會利用 {@link Cache} 移除尚未使用的 OTP。
     */
    private RecycleResponse cancelApplication(RecycleApplication applyForm) {
        RecycleStatus currentStatus = applyForm.getRecycleStatus();
        if (currentStatus != RecycleStatus.APPLIED
                && currentStatus != RecycleStatus.INSPECTING
                && currentStatus != RecycleStatus.WAITING_FOR_AGREEMENT) {
            throw new IllegalStateException("目前回收狀態不允許取消");
        }

        applyForm.setRecycleStatus(RecycleStatus.CANCELLED);
        applyForm.setLastModifiedTime(LocalDateTime.now());
        // 取消後立即移除可能尚未使用的 OTP，避免驗證碼繼續有效。
        Cache otpCache = cacheManager.getCache(CacheConfig.REGISTER_OTP_CACHE);
        if (otpCache != null) {
            otpCache.evict(agreementOtpKey(applyForm.getApplyId()));
        }
        return toResponse(recycleApplicationDao.save(applyForm));
    }

    /** 將回收單欄位轉成商品庫存資料，售價為回收估價加上 NT$1,500。 */
    private Product buildInventoryProduct(RecycleApplication applyForm, LocalDateTime createdTime) {
        Product product = new Product();
        product.setProductName(applyForm.getProductName());
        product.setCategory(applyForm.getCategory());
        product.setImageUrl(applyForm.getImageUrl());
        product.setDescription(Optional.ofNullable(applyForm.getDescription()).orElse(""));
        product.setAppearance(applyForm.getAppearance());
        product.setGrade(resolveProductGrade(applyForm.getAppearance()));
        product.setPrice(Math.addExact(applyForm.getEstimatedPrice(), 1_500));
        product.setSaleStatus(ProductSaleStatus.AVAILABLE);
        product.setCreatedTime(createdTime);
        product.setLastModifiedTime(createdTime);
        return product;
    }

    /** 將「95成新、9.5成新、9成新、90%」等外觀寫法換算成商品等級。 */
    private String resolveProductGrade(String appearance) {
        double conditionPercent = parseConditionPercent(appearance);
        if (conditionPercent >= 95) return "S級";
        if (conditionPercent >= 90) return "A級";
        if (conditionPercent >= 80) return "B級";
        if (conditionPercent >= 70) return "C級";
        throw new IllegalArgumentException("外觀須達七成新以上才能新增至商品庫存");
    }

    private double parseConditionPercent(String appearance) {
        if (appearance == null || appearance.isBlank()) {
            throw new IllegalArgumentException("回收單缺少外觀程度，無法判斷商品等級");
        }

        Matcher percentMatcher = CONDITION_PERCENT_PATTERN.matcher(appearance);
        if (percentMatcher.find()) {
            return Double.parseDouble(percentMatcher.group(1));
        }

        Matcher chengMatcher = CONDITION_CHENG_PATTERN.matcher(appearance);
        if (chengMatcher.find()) {
            double value = Double.parseDouble(chengMatcher.group(1));
            // 「9.5成新」代表 95%，同時依需求接受「95成新」直接表示 95%。
            return value <= 10 ? value * 10 : value;
        }

        String normalizedAppearance = appearance.replaceAll("\\s+", "");
        if (normalizedAppearance.contains("九五成新")) return 95;
        if (normalizedAppearance.contains("九成新")) return 90;
        if (normalizedAppearance.contains("八成新")) return 80;
        if (normalizedAppearance.contains("七成新")) return 70;

        throw new IllegalArgumentException("無法從外觀程度判斷商品等級");
    }

    private void verifyAgreementOtp(Long applyId, String submittedOtp) {
        Cache cache = getOtpCache();
        Cache.ValueWrapper wrapper = cache.get(agreementOtpKey(applyId));

        if (wrapper == null || !(wrapper.get() instanceof AgreementOtpEntry entry)) {
            throw new IllegalArgumentException("OTP 驗證碼錯誤或已逾期，請重新取得驗證碼");
        }

        synchronized (entry) {
            if (!entry.otp.equals(submittedOtp)) {
                entry.failedAttempts++;
                // 限制單組 OTP 最多嘗試 5 次，降低暴力猜測 6 位數驗證碼的風險。
                if (entry.failedAttempts >= 5) {
                    cache.evict(agreementOtpKey(applyId));
                }
                throw new IllegalArgumentException("OTP 驗證碼錯誤或已逾期，請重新取得驗證碼");
            }
        }

        // 驗證碼只能成功使用一次。
        cache.evict(agreementOtpKey(applyId));
    }

    private void validateSignature(String signatureDataUrl) {
        if (!signatureDataUrl.startsWith(PNG_DATA_URL_PREFIX)) {
            throw new IllegalArgumentException("電子簽名格式不正確");
        }

        try {
            byte[] signatureBytes = Base64.getDecoder().decode(
                    signatureDataUrl.substring(PNG_DATA_URL_PREFIX.length()));

            // Canvas 空白圖或異常內容通常極小；同時檢查 PNG 固定檔頭。
            if (signatureBytes.length < 200
                    || signatureBytes[0] != (byte) 0x89
                    || signatureBytes[1] != 0x50
                    || signatureBytes[2] != 0x4E
                    || signatureBytes[3] != 0x47) {
                throw new IllegalArgumentException("電子簽名內容不正確");
            }
            BufferedImage signatureImage = ImageIO.read(new ByteArrayInputStream(signatureBytes));
            if (signatureImage == null || !containsSignatureStroke(signatureImage)) {
                throw new IllegalArgumentException("請先完成電子簽名");
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("電子簽名內容不正確");
        }
    }

    /** 檢查透明 Canvas 中是否真的存在足量筆跡，避免空白 PNG 通過後端驗證。 */
    private boolean containsSignatureStroke(BufferedImage signatureImage) {
        int strokePixels = 0;

        for (int y = 0; y < signatureImage.getHeight(); y++) {
            for (int x = 0; x < signatureImage.getWidth(); x++) {
                int argb = signatureImage.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                if (alpha > 30 && (red < 240 || green < 240 || blue < 240)) {
                    strokePixels++;
                    if (strokePixels >= 50) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Cache getOtpCache() {
        Cache cache = cacheManager.getCache(CacheConfig.REGISTER_OTP_CACHE);
        if (cache == null) {
            throw new IllegalStateException("OTP 快取尚未設定");
        }
        return cache;
    }

    private String agreementOtpKey(Long applyId) {
        return AGREEMENT_OTP_KEY_PREFIX + applyId;
    }

    /** 快取內同時保存驗證碼與失敗次數，資料會依 CacheConfig 在 5 分鐘後自動失效。 */
    private static class AgreementOtpEntry {
        private final String otp;
        private int failedAttempts;

        private AgreementOtpEntry(String otp) {
            this.otp = otp;
        }
    }

    //實作修改回收單


    @Override
    public void updateApplyForm(Long applyId, RecycleRequest recycleRequest, MultipartFile imageFile) throws IOException {
        Optional<RecycleApplication> applyForm = recycleApplicationDao.findById(applyId);
        if(applyForm.isPresent()){
            RecycleApplication gotApplyForm = applyForm.get();
            // COMPLETED 已建立對應商品庫存，禁止再修改以免回收紀錄與商品資料不一致。
            if (gotApplyForm.getRecycleStatus() == RecycleStatus.COMPLETED) {
                throw new IllegalStateException("回收完成的申請不可再編輯");
            }
            gotApplyForm.setProductName(recycleRequest.getProductName());
            gotApplyForm.setCategory(recycleRequest.getCategory());
            gotApplyForm.setAppearance(recycleRequest.getAppearance());
            // 後台有選新檔案時才上傳並更新網址，否則保留原本的 Cloudinary 圖片。
            if (imageFile != null && !imageFile.isEmpty()) {
                gotApplyForm.setImageUrl(uploadImage(imageFile));
            }
            gotApplyForm.setDescription(recycleRequest.getDescription());
            gotApplyForm.setEstimatedPrice(recycleRequest.getEstimatedPrice());

            gotApplyForm.setLastModifiedTime(LocalDateTime.now());
            RecycleApplication updatedApplyForm = recycleApplicationDao.save(gotApplyForm);
        }else{
            return;
        }

    }

    /** 將後台選取的回收商品圖片上傳到獨立資料夾並回傳 HTTPS 網址。 */
    private String uploadImage(MultipartFile imageFile) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                imageFile.getBytes(),
                ObjectUtils.asMap("folder", "gigafix/recycle-applications", "resource_type", "image")
        );
        return (String) uploadResult.get("secure_url");
    }

    //實作刪除一筆回收單
    @Override
    public boolean deleteApplyFormById(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao
                .findByIdForUpdate(applyId)
                .orElse(null);
        if (applyForm == null) {
            return false;
        }
        if (applyForm.getRecycleStatus() != RecycleStatus.CANCELLED) {
            throw new IllegalStateException("只有已取消的回收單可以刪除");
        }

        recycleApplicationDao.delete(applyForm);
        return true;
    }
    //實作刪除所有回收單
    @Override
    public void deleteAllApplyForms() {
        List<RecycleApplication> applyForms = recycleApplicationDao.findAll();
        boolean containsActiveApplication = applyForms.stream()
                .anyMatch(application -> application.getRecycleStatus() != RecycleStatus.CANCELLED);
        if (containsActiveApplication) {
            throw new IllegalStateException("仍有未取消的回收單，不可執行全部刪除");
        }
        recycleApplicationDao.deleteAll(applyForms);
    }

    /**
     * 依照 ProductService 的 JSON 匯入模式，從 classpath 讀取固定 Demo 資料。
     * 此類別已標示 @Transactional；驗證或儲存任一步驟失敗時，整批匯入會回滾。
     */
    @Override
    public int importApplyForms() throws IOException {
        // JSON 放在 src/main/resources，打包成 JAR 後仍可透過 classpath 取得。
        ClassPathResource resource = new ClassPathResource("recycle-applications-demo.json");

        try (InputStream inputStream = resource.getInputStream()) {
            // 使用獨立匯入 DTO，避免 JSON 直接建立 Member、Stores 等 JPA 關聯物件。
            List<RecycleApplicationImportItem> importItems = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<RecycleApplicationImportItem>>() {}
            );

            // 先收集全部會員 ID，讓外鍵驗證可以一次完成。
            Set<Long> requiredMemberIds = new LinkedHashSet<>();
            for (RecycleApplicationImportItem item : importItems) {
                if (item.getMemberId() == null) {
                    throw new IllegalArgumentException("Demo 回收單缺少 memberId");
                }
                requiredMemberIds.add(item.getMemberId());
            }

            Map<Long, Member> membersById = new HashMap<>();
            for (Member member : memberRepository.findAllById(requiredMemberIds)) {
                membersById.put(member.getId(), member);
            }

            // 只要缺少任一會員便停止匯入，效果等同原 SQL 的前置檢查。
            Set<Long> missingMemberIds = new LinkedHashSet<>(requiredMemberIds);
            missingMemberIds.removeAll(membersById.keySet());
            if (!missingMemberIds.isEmpty()) {
                throw new IllegalStateException("會員 ID 不存在：" + missingMemberIds);
            }

            // DTO 全部轉成 Entity 後再批次寫入，避免逐筆 save 造成多次資料庫往返。
            List<RecycleApplication> applications = importItems.stream()
                    .map(item -> toImportEntity(item, membersById.get(item.getMemberId())))
                    .toList();

            recycleApplicationDao.saveAll(applications);
            return applications.size();
        }
    }

    /** 將匯入 DTO 轉成可儲存的 Entity，並補上已驗證的會員與門市關聯。 */
    private RecycleApplication toImportEntity(
            RecycleApplicationImportItem item,
            Member member
    ) {
        RecycleApplication application = new RecycleApplication();
        application.setMember(member);
        application.setProductName(item.getProductName());
        application.setCategory(item.getCategory());
        application.setAppearance(item.getAppearance());
        application.setImageUrl(item.getImageUrl());
        application.setDescription(item.getDescription());
        application.setEstimatedPrice(item.getEstimatedPrice());
        application.setRecycleStatus(item.getRecycleStatus());
        application.setAgreementSignature(item.getAgreementSignature());
        application.setAgreementSignedTime(parseImportDateTime(item.getAgreementSignedTime()));
        application.setCreatedTime(parseImportDateTime(item.getCreatedTime()));
        application.setLastModifiedTime(parseImportDateTime(item.getLastModifiedTime()));

        // Demo 可不指定門市；有指定時仍須驗證門市外鍵是否存在。
        if (item.getStoreId() != null) {
            Stores store = storesRepository.findById(item.getStoreId())
                    .orElseThrow(() -> new IllegalStateException(
                            "門市 ID 不存在：" + item.getStoreId()
                    ));
            application.setStores(store);
        }

        return application;
    }

    /**
     * 將 JSON 的 ISO 8601 時區字串轉成資料表使用的 LocalDateTime。
     * null 代表尚未簽署等允許空值的時間欄位。
     */
    private LocalDateTime parseImportDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value).toLocalDateTime();
    }

    //將全部回收單轉為 DTO 後匯出，避免直接序列化 Member、Stores 的關聯資料
    @Override
    public byte[] exportApplyForms() throws IOException {
        List<RecycleResponse> applyForms = recycleApplicationDao
                .findAll(Sort.by("createdTime").descending())
                .stream()
                .map(this::toResponse)
                .toList();

        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(applyForms);
    }

    // 匯出 DTO 而非直接處理關聯 Entity，避免把會員、門市或簽名影像寫入 Excel。
    @Override
    public byte[] exportApplyFormsExcel() throws IOException {
        List<RecycleResponse> applyForms = recycleApplicationDao
                .findAll(Sort.by("createdTime").descending())
                .stream()
                .map(this::toResponse)
                .toList();
        String[] headers = {
                "回收單 ID", "會員 ID", "會員姓名", "聯絡電話", "商品名稱", "類別",
                "外觀狀況", "圖片網址", "描述", "估價", "回收狀態", "同意簽署時間",
                "申請時間", "最後修改時間", "門市 ID", "門市名稱"
        };
        int[] columnWidths = {14, 12, 18, 18, 24, 14, 30, 45, 40, 14, 20, 22, 22, 22, 12, 22};

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("回收單資料");
            sheet.createFreezePane(0, 1);

            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, columnWidths[index] * 256);
            }

            for (int index = 0; index < applyForms.size(); index++) {
                RecycleResponse application = applyForms.get(index);
                Row row = sheet.createRow(index + 1);
                setNumberCell(row, 0, application.getApplyId());
                setNumberCell(row, 1, application.getMemberId());
                setTextCell(row, 2, application.getMemberName());
                setTextCell(row, 3, application.getContactPhone());
                setTextCell(row, 4, application.getProductName());
                setTextCell(row, 5, application.getCategory() == null ? null : application.getCategory().name());
                setTextCell(row, 6, application.getAppearance());
                setTextCell(row, 7, application.getImageUrl());
                setTextCell(row, 8, application.getDescription());
                setNumberCell(row, 9, application.getEstimatedPrice());
                setTextCell(row, 10, application.getRecycleStatus() == null ? null : application.getRecycleStatus().name());
                setTextCell(row, 11, formatExcelDateTime(application.getAgreementSignedTime()));
                setTextCell(row, 12, formatExcelDateTime(application.getCreatedTime()));
                setTextCell(row, 13, formatExcelDateTime(application.getLastModifiedTime()));
                setNumberCell(row, 14, application.getStoreId());
                setTextCell(row, 15, application.getStoreName());
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private CellStyle createExcelHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setTextCell(Row row, int columnIndex, String value) {
        row.createCell(columnIndex).setCellValue(value == null ? "" : value);
    }

    private void setNumberCell(Row row, int columnIndex, Number value) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private String formatExcelDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXCEL_DATE_TIME_FORMAT);
    }
}
