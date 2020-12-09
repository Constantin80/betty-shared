package info.fmro.shared.betapi;

import info.fmro.shared.entities.CancelExecutionReport;
import info.fmro.shared.entities.CancelInstruction;
import info.fmro.shared.enums.ExecutionReportStatus;
import info.fmro.shared.enums.InstructionReportErrorCode;
import info.fmro.shared.objects.TemporaryOrder;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CancelOrdersThread
        implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CancelOrdersThread.class);
    private final String marketId;
    @NotNull
    private final List<CancelInstruction> cancelInstructionsList;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    @NotNull
    private final TemporaryOrder temporaryOrder;
    @NotNull
    private final Method sendPostRequestRescriptMethod;

    @Contract(pure = true)
    public CancelOrdersThread(final String marketId, @NotNull final List<CancelInstruction> cancelInstructionsList, @NotNull final TemporaryOrder temporaryOrder, @NotNull final Method sendPostRequestRescriptMethod) {
        this.marketId = marketId;
        this.cancelInstructionsList = new ArrayList<>(cancelInstructionsList);
        this.temporaryOrder = temporaryOrder;
        this.sendPostRequestRescriptMethod = sendPostRequestRescriptMethod;
    }

    @Override
    public void run() {
        final boolean success;
        if (this.marketId != null && !this.cancelInstructionsList.isEmpty() && this.cancelInstructionsList.size() <= 60) {
            final RescriptResponseHandler rescriptResponseHandler = new RescriptResponseHandler();
            final CancelExecutionReport cancelExecutionReport = ApiNgRescriptOperations.cancelOrders(this.marketId, this.cancelInstructionsList, null, rescriptResponseHandler, this.sendPostRequestRescriptMethod);
            if (cancelExecutionReport != null) {
                final ExecutionReportStatus executionReportStatus = cancelExecutionReport.getStatus();
                if (executionReportStatus == ExecutionReportStatus.SUCCESS) {
                    logger.info("canceled orders: {} {} {} {} p:{}", this.temporaryOrder.getReasonId(), Generic.objectToString(cancelExecutionReport), this.temporaryOrder.getRunnerId(), this.temporaryOrder.getSide(), this.temporaryOrder.getPrice());

//                    if (Statics.safeBetModuleActivated) {
//                        PlacedAmountsThread.shouldCheckAmounts.set(true);
//                    } else { // PlacedAmountsThread is part of safeBetModule, and is not needed if I have stream access
//                    }

                    success = true;
                } else {
                    @NotNull final HashSet<InstructionReportErrorCode> instructionErrorCodes = cancelExecutionReport.getInstructionErrorCodes();
                    if (instructionErrorCodes.size() == 1 && instructionErrorCodes.contains(InstructionReportErrorCode.BET_TAKEN_OR_LAPSED)) {
                        logger.info("bet taken or lapsed in cancelOrders: {} {} {} {}", Generic.objectToString(cancelExecutionReport), this.marketId, Generic.objectToString(this.cancelInstructionsList), Generic.objectToString(this.temporaryOrder));
                    } else {
                        logger.error("!!!no success in cancelOrders: {} {} {} {}", Generic.objectToString(cancelExecutionReport), this.marketId, Generic.objectToString(this.cancelInstructionsList), Generic.objectToString(this.temporaryOrder));
                    }
                    success = false;
                }
            } else {
                logger.error("!!!failed to cancelOrders: {} {} {}", this.marketId, Generic.objectToString(this.cancelInstructionsList), Generic.objectToString(this.temporaryOrder));
                success = false;
            }
        } else {
            logger.error("STRANGE variables in CancelOrdersThread: {} {} {}", this.marketId, Generic.objectToString(this.cancelInstructionsList), Generic.objectToString(this.temporaryOrder));
            success = false;
        }

        if (success) { // nothing to be done, I just act in case of failure
        } else {
//            this.temporaryOrder.setExpirationTime(System.currentTimeMillis() + Generic.MINUTE_LENGTH_MILLISECONDS); // expiration is set by default now
        }
    }
}
