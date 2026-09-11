package com.gigafix.product.service;

import com.gigafix.common.config.CacheConfig;
import com.gigafix.member.entity.Member;
import com.gigafix.member.exception.MemberNotFoundException;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.product.Utils;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.RecycleStatus;
import com.gigafix.product.dto.RecycleAgreementRequest;
import com.gigafix.product.dto.RecycleQueryParams;
import com.gigafix.product.dto.RecycleRequest;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.product.repository.RecycleApplicationDao;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.StoresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class RecycleApplicationServiceImpl implements RecycleApplicationService{
    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final String AGREEMENT_OTP_KEY_PREFIX = "recycle-agreement:";
    private static final String PNG_DATA_URL_PREFIX = "data:image/png;base64,";

    @Autowired
    private RecycleApplicationDao recycleApplicationDao;
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
        applyForm.setImageUrl(recycleRequest.getImageUrl());
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

    @Override
    public boolean sendAgreementOtp(Long applyId) {
        RecycleApplication applyForm = recycleApplicationDao.findById(applyId).orElse(null);
        if (applyForm == null) {
            return false;
        }

        // 完成現場檢測且已有估價後，才可請客戶進行 OTP 與簽名確認。
        if (applyForm.getRecycleStatus() != RecycleStatus.INSPECTING) {
            throw new IllegalStateException("只有現場檢測評估中的回收單可以寄送同意驗證碼");
        }
        if (applyForm.getEstimatedPrice() == null) {
            throw new IllegalStateException("請先完成回收估價再寄送同意驗證碼");
        }

        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        recycleApplicationNotificationService.sendAgreementOtp(applyForm, otp);

        // 沿用組長設定的 5 分鐘 Caffeine 快取，以前綴隔離註冊 OTP 與回收同意 OTP。
        getOtpCache().put(agreementOtpKey(applyId), new AgreementOtpEntry(otp));
        return true;
    }

    @Override
    public RecycleResponse confirmAgreement(Long applyId, RecycleAgreementRequest request) {
        RecycleApplication applyForm = recycleApplicationDao.findById(applyId).orElse(null);
        if (applyForm == null) {
            return null;
        }

        if (applyForm.getRecycleStatus() != RecycleStatus.INSPECTING) {
            throw new IllegalStateException("只有現場檢測評估中的回收單可以確認估價同意");
        }

        validateSignature(request.getSignatureDataUrl());
        verifyAgreementOtp(applyId, request.getOtp());

        // OTP 與電子簽名都驗證通過後，才正式推進到狀態 3。
        applyForm.setRecycleStatus(RecycleStatus.WAITING_FOR_AGREEMENT);
        applyForm.setLastModifiedTime(LocalDateTime.now());
        return toResponse(recycleApplicationDao.save(applyForm));
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
    public void updateApplyForm(Long applyId, RecycleRequest recycleRequest) {
        Optional<RecycleApplication> applyForm = recycleApplicationDao.findById(applyId);
        if(applyForm.isPresent()){
            RecycleApplication gotApplyForm = applyForm.get();
            gotApplyForm.setProductName(recycleRequest.getProductName());
            gotApplyForm.setCategory(recycleRequest.getCategory());
            gotApplyForm.setAppearance(recycleRequest.getAppearance());
            gotApplyForm.setImageUrl(recycleRequest.getImageUrl());
            gotApplyForm.setDescription(recycleRequest.getDescription());
            gotApplyForm.setEstimatedPrice(recycleRequest.getEstimatedPrice());

            gotApplyForm.setLastModifiedTime(LocalDateTime.now());
            RecycleApplication updatedApplyForm = recycleApplicationDao.save(gotApplyForm);
        }else{
            return;
        }

    }

    //實作刪除一筆回收單
    @Override
    public void deleteApplyFormById(Long applyId) {
        recycleApplicationDao.deleteById(applyId);

    }
    //實作刪除所有回收單
    @Override
    public void deleteAllApplyForms() {
        recycleApplicationDao.deleteAll();
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
}
